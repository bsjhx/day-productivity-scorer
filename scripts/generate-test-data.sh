#!/bin/bash

# Generate test data for 6 months before today
# Calls http://localhost:8080/day/ POST endpoint with random scores
# Skips approximately 10% of days

API_URL="http://localhost:8080/day/"
END_DATE="2026-07-05"

# Calculate start date (6 months before)
if [[ "$OSTYPE" == "darwin"* ]]; then
    START_DATE=$(date -v-6m -j -f "%Y-%m-%d" "$END_DATE" "+%Y-%m-%d")
else
    START_DATE=$(date -d "$END_DATE - 6 months" "+%Y-%m-%d")
fi

echo "Generating test data from $START_DATE to $END_DATE"
echo ""

# Convert to seconds for iteration
if [[ "$OSTYPE" == "darwin"* ]]; then
    START_SEC=$(date -j -f "%Y-%m-%d" "$START_DATE" "+%s")
    END_SEC=$(date -j -f "%Y-%m-%d" "$END_DATE" "+%s")
else
    START_SEC=$(date -d "$START_DATE" "+%s")
    END_SEC=$(date -d "$END_DATE" "+%s")
fi

TOTAL=0
SKIPPED=0
POSTED=0

CURRENT=$START_SEC
while [ $CURRENT -le $END_SEC ]; do
    if [[ "$OSTYPE" == "darwin"* ]]; then
        DAY=$(date -r $CURRENT "+%Y-%m-%d")
    else
        DAY=$(date -d "@$CURRENT" "+%Y-%m-%d")
    fi

    TOTAL=$((TOTAL + 1))

    # Skip ~10% of days randomly
    if [ $((RANDOM % 10)) -eq 0 ]; then
        echo "⏭  Skipped: $DAY"
        SKIPPED=$((SKIPPED + 1))
    else
        # Random score 1-5
        SCORE=$((RANDOM % 5 + 1))

        # Call API
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL" \
            -H "Content-Type: application/json" \
            -d "{\"day\":\"$DAY\",\"score\":$SCORE}")

        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
            echo "✅ Posted: $DAY = $SCORE"
            POSTED=$((POSTED + 1))
        else
            echo "❌ Failed: $DAY = $SCORE (HTTP $HTTP_CODE)"
        fi
    fi

    # Next day
    CURRENT=$((CURRENT + 86400))
done

echo ""
echo "Summary: $TOTAL days total, $POSTED posted, $SKIPPED skipped"
