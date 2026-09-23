

# To Fix

Figure out IDs (should it be UUIDs or something)?

Fix OrderProcessor and logic overall




# Ordering example

Start with a big cash holding

When buying a stock, add a + stock holding and - money holding
When selling a stock, add a - stock holding and + money holding

Store all holdings

First in first out? Or you can allow 

Summary


For sell order, if we let user choose which holding to sell, that's not currently possible because the order doesn't have a holding id

## Upsides and downsides of storing anything calculatable in the backend database
- One option: use nightly recalculation of calculated columns; 
so it's non-normalized during the day and gets renormalized once per day or once per hour or something.
- Materialized View?