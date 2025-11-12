package model;

import java.io.Serializable;
import java.util.regex.Pattern;

public class UrlMatcher implements Serializable {
    private String protocol; // "Any", "HTTP", "HTTPS", or null/empty
    private String host; // Can be regex
    private String port; // Can be regex
    private String path; // Can be regex
    private boolean enabled;

    public UrlMatcher() {
        this.protocol = "Any";
        this.host = "";
        this.port = "";
        this.path = "";
        this.enabled = true;
    }

    public UrlMatcher(String protocol, String host, String port, String path) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.enabled = true;
    }

    public boolean matches(String url) {
        try {
            // Extract protocol
            String urlProtocol = "";
            String remainder = url;
            if (url.contains("://")) {
                urlProtocol = url.substring(0, url.indexOf("://")).toLowerCase();
                remainder = url.substring(url.indexOf("://") + 3);
            }

            // Check protocol
            if (protocol != null && !protocol.isEmpty() && !protocol.equalsIgnoreCase("Any")) {
                if (!urlProtocol.equalsIgnoreCase(protocol)) {
                    return false;
                }
            }

            // Extract host and port
            String urlHost = "";
            String urlPort = "";
            String urlPath = "";

            int slashIndex = remainder.indexOf('/');
            String hostPort = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
            urlPath = slashIndex >= 0 ? remainder.substring(slashIndex) : "/";

            // Strip query string from path
            int queryIndex = urlPath.indexOf('?');
            if (queryIndex >= 0) {
                urlPath = urlPath.substring(0, queryIndex);
            }

            if (hostPort.contains(":")) {
                int colonIndex = hostPort.lastIndexOf(':');
                urlHost = hostPort.substring(0, colonIndex);
                urlPort = hostPort.substring(colonIndex + 1);
            } else {
                urlHost = hostPort;
            }

            // Check host
            if (host != null && !host.isEmpty()) {
                if (!matchesPattern(urlHost, host)) {
                    return false;
                }
            }

            // Check port
            if (port != null && !port.isEmpty()) {
                if (!matchesPattern(urlPort, port)) {
                    return false;
                }
            }

            // Check path
            if (path != null && !path.isEmpty()) {
                if (!matchesPattern(urlPath, path)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesPattern(String value, String pattern) {
        try {
            // Use find() for partial matching - users can use ^ and $ for anchoring
            return Pattern.compile(pattern).matcher(value).find();
        } catch (Exception e) {
            // If it's not a valid regex, try substring match
            return value.contains(pattern);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (protocol != null && !protocol.isEmpty() && !protocol.equalsIgnoreCase("Any")) {
            sb.append(protocol.toLowerCase()).append("://");
        }
        if (host != null && !host.isEmpty()) {
            sb.append(host);
        } else {
            sb.append("*");
        }
        if (port != null && !port.isEmpty()) {
            sb.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        return sb.toString();
    }

    // Getters and setters
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

