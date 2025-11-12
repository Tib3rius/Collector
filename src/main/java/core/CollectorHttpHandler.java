package core;

import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class CollectorHttpHandler implements HttpHandler {
    private final BucketManager bucketManager;

    public CollectorHttpHandler(BucketManager bucketManager) {
        this.bucketManager = bucketManager;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Collect tokens from requests
        String url = requestToBeSent.url();
        String requestContent = requestToBeSent.toString();
        bucketManager.collectTokens(requestContent, url, requestToBeSent.toolSource().toolType(), true);

        // Apply replacements to requests using Montoya API
        HttpRequest modifiedRequest = bucketManager.applyReplacementsToRequest(requestToBeSent, requestToBeSent.toolSource().toolType());

        if (!modifiedRequest.toString().equals(requestToBeSent.toString())) {
            return RequestToBeSentAction.continueWith(modifiedRequest);
        }

        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Collect tokens from responses
        String url = responseReceived.initiatingRequest().url();
        String responseContent = responseReceived.toString();
        bucketManager.collectTokens(responseContent, url, responseReceived.toolSource().toolType(), false);

        // Apply replacements to responses using Montoya API
        HttpResponse modifiedResponse = bucketManager.applyReplacementsToResponse(responseReceived, responseReceived.toolSource().toolType());

        if (!modifiedResponse.toString().equals(responseReceived.toString())) {
            return ResponseReceivedAction.continueWith(modifiedResponse);
        }

        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
