# Smart Laundry Stock Predictor

Final project for the Building AI course

## Summary

The Smart Laundry Stock Predictor uses AI to forecast the daily consumption of laundry supplies like detergents and softeners based on weather and calendar data. It helps small laundry businesses optimize their inventory, preventing stockouts and reducing unnecessary storage costs.

## Background

Many small laundry businesses struggle with stock management. Customer demand fluctuates significantly based on weather, weekends, and local holidays. Running out of stock halts the business operations entirely, while overstocking eats up valuable storage space and cash flow. 
As someone who has built a Laundry Stock Management App before, I noticed that predicting exactly when to reorder supplies is a common and frequent headache for business owners.

## How is it used?

The solution is integrated directly into the laundry's point-of-sale or management app. At the end of each day, the system checks the current inventory levels and the weather forecast for the upcoming week. The users are the laundry owners or managers who will receive an automated alert (e.g., a push notification) specifying exactly what supplies to buy and in what quantities before the weekend rush.

## Data sources and AI methods

The data comes from the laundry's own historical point-of-sale records. The dataset includes:
- Daily number of orders and total weight of laundry (kg).
- Weather conditions (sunny, cloudy, rainy) fetched from a public weather API.
- Calendar data (day of the week, local holidays).
- Daily consumption rate of detergents and softeners.

The AI method used is **Linear Regression** or a simple **Neural Network**. The model takes the weather forecast and calendar data as input features (X) to predict the expected consumption of supplies (y) for the next few days.

## Challenges

The project does not solve unpredictable supply chain issues (e.g., if the supplier runs out of stock). It also cannot predict sudden machine breakdowns that might halt operations. An ethical consideration is ensuring that customer data (if used to predict order volume) is completely anonymized and aggregated so that no personal identities are stored or processed by the AI model.

## What next?

The project could grow into a fully automated supply chain system where the AI not only predicts the stock needs but also automatically places orders with suppliers via an API. To move on, I would need skills in API integrations with wholesale suppliers and a larger dataset from multiple laundry branches to make the prediction model more robust.

## Acknowledgments

* Inspired by my own previous work on a Laundry Stock Management App.
* Course material from Building AI (Elements of AI).
