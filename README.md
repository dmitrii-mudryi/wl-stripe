# Payment Application

This is a Spring Boot-based web application that integrates with Stripe to facilitate card payments. The application consists of a backend server that handles payment requests and communicates with Stripe, and a frontend client that provides a simple UI for users to enter payment details.

## Demo Video
[![Watch the video](https://img.youtube.com/vi/9ij8b-z1KsA/maxresdefault.jpg)](https://youtu.be/9ij8b-z1KsA)

## Table of Contents

- Prerequisites
- Configuration
- Running the Application
- Making a Payment
- Handling Webhooks
- Logging
- Troubleshooting

## Prerequisites

Before setting up and running the application, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
- **Maven 3.6+ (included)**
- **Stripe Account:** You need to create a Stripe account to obtain your API keys or use existing one for testing.
- **Ngrok (Optional):** If you want to test webhooks locally, you can use Ngrok to expose your local server to the internet.

## Configuration

### Stripe API Keys

To integrate with Stripe, you'll need to configure your API keys in the application. These keys include:

- **Secret Key:** Used by the server to authenticate API requests to Stripe.
- **Publishable Key:** Used by the frontend to securely create payment methods.

You can find these keys in your Stripe Dashboard.

### Application Properties

To configure the application, you will need to set the following properties in the `application.properties` file located in `src/main/resources/`:

```properties
stripe.api.key=your_stripe_secret_key
stripe.webhook.secret=your_stripe_webhook_secret
```

Replace `your_stripe_secret_key` and `your_stripe_webhook_secret` with your actual Stripe secret key and webhook secret.

## Frontend Configuration

The frontend requires the Stripe publishable key, which you can set directly in the `index.html` file:

```html
<script src="https://js.stripe.com/v3/"></script>
<script>
    var stripe = Stripe('your_publishable_key');
</script>
```

Replace `your_publishable_key` with your actual Stripe publishable key.

## Running the Application

To run the application:

### Build the Project

Use Maven to build the project.

```bash
./mvnw clean install
```

### Run the Application

Start the Spring Boot application using the following command:

```bash
./mvnw spring-boot:run
```

The application will start on the default port 8080. You can access it by navigating to `https://wl-stripe-app.ngrok.app/` in your web browser.

You can also turn off Ngrok by updating following property and run the application locally by navigating to `http://localhost:8080`.
This will disable the Ngrok tunneling and the application will run on the local server  without possibility to retrieve webhook.

```properties
nrgok.enabled=false
```

## Making a Payment

### Steps to Make a Payment:

1. **Open the Payment Form:** Navigate to `https://wl-stripe-app.ngrok.app/` to access the payment form.
2. **Enter Payment Details:**
    - **Name:** Enter the payer's name.
    - **Email:** Enter the payer's email.
    - **Amount:** Specify the amount to be charged (in USD by default).
    - **Currency:** Select the currency from the dropdown.
    - **Card Details:** Enter card details (number, expiry date, CVC).
    - **Simulate Webhook failure:** Check if you want to simulate failure on working server https://wl-stripe-app.ngrok.app/
3. **Submit Payment:** Click the "Pay" button to submit the payment.

### Payment Flow:

- **Client Side:** The frontend interacts with Stripe to create a `PaymentMethod` and sends it to the backend server.
- **Server Side:** The backend server processes the payment by creating a `PaymentIntent` and confirming it.
- **Success/Failure:** The server responds with the payment status, which is displayed on the UI.

### Supported Test Cards

For testing purposes, you can use the following Stripe test card details:

#### Success
- **Card Number:** `4242 4242 4242 4242`
- **Expiry Date:** Any future date
- **CVC:** Any 3-digit number

#### Fail (Insufficient funds)
- **Card Number:** `4000 0000 0000 9995`
- **Expiry Date:** Any future date
- **CVC:** Any 3-digit number

More cards: https://stripe.com/docs/testing

## Handling Webhooks

### Webhook Configuration

To receive payment status updates from Stripe (e.g., payment success, failure), you need to configure a webhook in your Stripe Dashboard.
You may change `https://wl-stripe-app.ngrok.app/` to your domain.

```ruby
https://wl-stripe-app.ngrok.app/api/webhook
```

## Webhook Events

The application listens for the following Stripe events:

- `payment_intent.succeeded`
- `payment_intent.payment_failed`

Stripe sends these events to the webhook URL, and the application processes them to update the payment status in the database.

## Logging

### Log Configuration

The application is configured to use SLF4J with Logback for logging. Logs are written to both the console and a file located at `logs/app.log`.

### Log Levels

- **INFO:** General application flow.
- **ERROR:** Exceptions and errors during payment processing.

### Customizing Logging

You can customize logging behavior by modifying the file `application.properties` or create own config `logback-spring.xml`.

## Troubleshooting

### Common Issues

- **Invalid API Key:** Ensure that you are using the correct Stripe API keys in both the backend and frontend.
- **Webhook Not Triggering:** If you're testing locally, make sure Ngrok is running and the webhook URL is correctly configured.
- **Payment Failed:** Check the logs for detailed error messages. Common issues include incorrect card details or insufficient funds.

### Debugging

- **Check Logs:** Review `logs/app.log` for detailed error information.
- **Console Output:** Monitor the console for real-time logging output during development.

### Contact Support

If you encounter issues not covered in this README, refer to the Stripe Documentation or reach out to the development team for assistance.
