package com.ceo.trading_platform_backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MarketService {

	private static final int MAX_BATCH_QUOTES = 25;
	private static final String API_KEY_HEADER = "X-Api-Key";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;

	public MarketService(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${market.api.base-url:https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1}") String baseUrl,
			@Value("${api_key:}") String apiKey) {
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeader("accept", "*/*")
				.build();
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
	}

	// Returns the latest numeric price for a single symbol.
	public BigDecimal getPrice(String symbol) {
		return getQuote(symbol).price();
	}

	// Fetches the full latest quote payload for a single symbol.
	public QuoteData getQuote(String symbol) {
		QuoteResponse response = getAndRead("/quotes/{symbol}", QuoteResponse.class, normalizeSymbol(symbol));
		return response.data();
	}

	// Fetches quote data for up to 25 symbols in a single network call.
	public List<BatchQuoteResult> getQuotes(List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			throw new IllegalArgumentException("At least one symbol is required.");
		}
		if (symbols.size() > MAX_BATCH_QUOTES) {
			throw new IllegalArgumentException("The quotes endpoint accepts at most 25 symbols per request.");
		}

		String joinedSymbols = symbols.stream()
				.map(this::normalizeSymbol)
				.reduce((left, right) -> left + "," + right)
				.orElseThrow();

		BatchQuotesResponse response = restClient.get()
				.uri(uriBuilder -> uriBuilder.path("/quotes")
						.queryParam("symbols", joinedSymbols)
						.build())
				.header(API_KEY_HEADER, requireApiKey())
				.exchange((request, httpResponse) -> readResponse(httpResponse, BatchQuotesResponse.class));

		return response.data().quotes();
	}

	// Fetches stored metadata for a symbol, including coverage dates and active status.
	public SymbolMetadataData getSymbolMetadata(String symbol) {
		SymbolMetadataResponse response = getAndRead("/symbols/{symbol}", SymbolMetadataResponse.class, normalizeSymbol(symbol));
		return response.data();
	}

	// Fetches daily candles for a symbol over an optional date range.
	public List<Candle> getCandles(String symbol, LocalDate from, LocalDate to) {
		CandlesResponse response = restClient.get()
				.uri(uriBuilder -> {
					var builder = uriBuilder.path("/candles/{symbol}")
							.queryParam("interval", "1d");
					if (from != null) {
						builder = builder.queryParam("from", from);
					}
					if (to != null) {
						builder = builder.queryParam("to", to);
					}
					return builder.build(normalizeSymbol(symbol));
				})
				.header(API_KEY_HEADER, requireApiKey())
				.exchange((request, httpResponse) -> readResponse(httpResponse, CandlesResponse.class));

		return response.data().candles();
	}

	// Convenience check to confirm the API still recognizes a symbol as active.
	public boolean isActiveSymbol(String symbol) {
		return Boolean.TRUE.equals(getSymbolMetadata(symbol).active());
	}

	// Performs a simple GET request and deserializes a successful JSON response.
	private <T> T getAndRead(String path, Class<T> responseType, Object... uriVariables) {
		return restClient.get()
				.uri(path, uriVariables)
				.header(API_KEY_HEADER, requireApiKey())
				.exchange((request, httpResponse) -> readResponse(httpResponse, responseType));
	}

	// Converts HTTP responses into typed payloads or domain-specific exceptions.
	private <T> T readResponse(ClientHttpResponse response, Class<T> responseType) throws IOException {
		HttpStatusCode statusCode = response.getStatusCode();
		String body = readBody(response);

		if (statusCode.value() == HttpStatus.ACCEPTED.value()) {
			Integer retryAfterSeconds = parseRetryAfter(response.getHeaders().getFirst("Retry-After"));
			throw new MarketDataPendingException(retryAfterSeconds, extractErrorMessage(body, "Market data is still being prepared."));
		}

		if (statusCode.is2xxSuccessful()) {
			return objectMapper.readValue(body, responseType);
		}

		if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
			throw new MarketSymbolNotFoundException(extractErrorMessage(body, "Symbol was not recognized by the market API."));
		}

		if (statusCode.value() == HttpStatus.UNAUTHORIZED.value() || statusCode.value() == HttpStatus.FORBIDDEN.value()) {
			throw new MarketAuthenticationException(extractErrorMessage(body, "The market API rejected the configured credentials."));
		}

		if (statusCode.is4xxClientError()) {
			throw new MarketClientException(statusCode.value(), extractErrorMessage(body, "The market API rejected the request."));
		}

		if (statusCode.is5xxServerError()) {
			throw new MarketServerException(statusCode.value(), extractErrorMessage(body, "The market API failed while processing the request."));
		}

		throw new MarketClientException(statusCode.value(), "Unexpected response from market API.");
	}

	// Reads the raw response body once so it can be parsed or included in exception messages.
	private String readBody(ClientHttpResponse response) throws IOException {
		try (InputStream bodyStream = response.getBody()) {
			if (bodyStream == null) {
				return "";
			}
			return new String(bodyStream.readAllBytes());
		}
	}

	// Pulls a human-readable error message from the API error envelope when one exists.
	private String extractErrorMessage(String body, String fallbackMessage) {
		if (body == null || body.isBlank()) {
			return fallbackMessage;
		}

		try {
			ErrorEnvelope errorEnvelope = objectMapper.readValue(body, ErrorEnvelope.class);
			if (errorEnvelope.error() != null && errorEnvelope.error().message() != null && !errorEnvelope.error().message().isBlank()) {
				return errorEnvelope.error().message();
			}
		} catch (Exception ignored) {
		}

		return fallbackMessage;
	}

	// Normalizes caller input to the uppercase symbols returned by the API.
	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("Symbol is required.");
		}
		return symbol.trim().toUpperCase();
	}

	// Ensures an API key is configured before any outbound request is attempted.
	private String requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("market.api.key must be configured before calling the market API.");
		}
		return apiKey;
	}

	// Parses the Retry-After header when the API says data is still warming up.
	private Integer parseRetryAfter(String retryAfterHeader) {
		if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
			return null;
		}

		try {
			return Integer.valueOf(retryAfterHeader);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record QuoteResponse(QuoteData data, ApiMeta meta) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record QuoteData(
			String symbol,
			BigDecimal price,
			BigDecimal bid,
			BigDecimal ask,
			BigDecimal spreadBps,
			String currency,
			BigDecimal change,
			BigDecimal changePercent,
			BigDecimal previousClose,
			OffsetDateTime asOf,
			String marketState) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record BatchQuotesResponse(BatchQuotesData data, ApiMeta meta) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record BatchQuotesData(List<BatchQuoteResult> quotes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record BatchQuoteResult(String symbol, String source, Boolean stale, QuoteData quote, ApiError error) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record SymbolMetadataResponse(SymbolMetadataData data, ApiMeta meta) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SymbolMetadataData(
			String symbol,
			String name,
			String type,
			String exchange,
			String currency,
			Boolean active,
			Coverage coverage) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Coverage(LocalDate eodFrom, LocalDate eodTo) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CandlesResponse(CandlesData data, ApiMeta meta) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record CandlesData(String symbol, String interval, String currency, List<Candle> candles) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Candle(
			LocalDate date,
			BigDecimal open,
			BigDecimal high,
			BigDecimal low,
			BigDecimal close,
			BigDecimal adjclose,
			Long volume,
			Boolean synthetic) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ApiMeta(
			OffsetDateTime asOf,
			String disclaimer,
			String symbol,
			String source,
			Boolean stale,
			Boolean partial,
			LocalDate availableFrom,
			String spreadSource) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ErrorEnvelope(ApiError error) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ApiError(String code, String message, Map<String, Object> details) {
	}

	public static class MarketClientException extends RuntimeException {

		public MarketClientException(int statusCode, String message) {
			super("Market API returned " + statusCode + ": " + message);
		}
	}

	public static class MarketAuthenticationException extends RuntimeException {

		public MarketAuthenticationException(String message) {
			super(message);
		}
	}

	public static class MarketSymbolNotFoundException extends RuntimeException {

		public MarketSymbolNotFoundException(String message) {
			super(message);
		}
	}

	public static class MarketDataPendingException extends RuntimeException {

		private final Integer retryAfterSeconds;

		public MarketDataPendingException(Integer retryAfterSeconds, String message) {
			super(message);
			this.retryAfterSeconds = retryAfterSeconds;
		}

		public Integer getRetryAfterSeconds() {
			return retryAfterSeconds;
		}
	}

	public static class MarketServerException extends RuntimeException {

		public MarketServerException(int statusCode, String message) {
			super("Market API returned " + statusCode + ": " + message);
		}
	}
}
