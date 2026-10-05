# Tests

## getPortfolioById()
Get correct portfolio when it exists
Throw ResourceNotFoundException on ID that doesn't exist
## getPortfolioResponseById()
Get correct response 
Throw ResourceNotFoundException on ID that doesn't exist
## getBuyingPowerByPortfolioId()
Throw ResourceNotFoundException on ID that doesn't exist
Get 0 when no cash holdings
Get correct number on 1 cash holding
Get correct number on 2+ cash holdings
## getTotalValueByPortfolioId()
** Mock MarketService **
Throw ResourceNotFoundException on ID that doesn't exist
Get 0 when no holdings
Get correct number when only cash holdings 
Get correct number when cash and 1 non-cash holding
Get correct number when cash and 2+ non-cash holding
## getHoldingsByPortfolioId()
Throw ResourceNotFoundException on ID that doesn't exist
Get empty list on no holdings
Get list with 1 holding on 1 holding
Get list with 2+ holdings on 2+ holdings
## updateHoldingsFromOrder()
Throw ResourceNotFoundException on ID that doesn't exist
