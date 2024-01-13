import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class Downloader {

    private static Path downloadDirectory; // Add this variable

    public static void setDownloadDirectory(Path directory) {
        downloadDirectory = directory;
    }

    public static void downloadFromLibgenMirror(String mirrorUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest headRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mirrorUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> headResponse = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
            String contentType = headResponse.headers().firstValue("Content-Type").orElse("");

            if (contentType.startsWith("text/html")) {
                Document document = Jsoup.connect(mirrorUrl).get();
                String directDownloadLink = null;

                // Logic to extract the download link
                if (mirrorUrl.contains("library.lol")) {
                    Element downloadLinkElement = document.selectFirst("h2 > a[href]");
                    if (downloadLinkElement != null) {
                        directDownloadLink = downloadLinkElement.attr("href");
                    }
                } else {
                    Element downloadLinkElement = document.selectFirst("td[bgcolor=#A9F5BC] a[href^=https://cdn]");
                    if (downloadLinkElement != null) {
                        directDownloadLink = downloadLinkElement.attr("href");
                    }
                }

                if (directDownloadLink != null) {
                    System.out.println("Direct Download Link: " + directDownloadLink);
                    downloadFile(directDownloadLink); // Download the file using Java's HTTP client
                } else {
                    System.out.println("Direct download link not found on the mirror page.");
                    openInBrowser(mirrorUrl);
                }
            } else {
                System.out.println("Non-HTML content, downloading file directly.");
                downloadFile(mirrorUrl); // Directly download if the content is not HTML
            }
        } catch (UnsupportedMimeTypeException e) {
            // This exception indicates a direct file download link, not HTML content.
            System.out.println("Direct file download link detected, bypassing HTML parsing.");
            downloadFile(mirrorUrl); // Directly download the file without parsing as HTML
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFromLibraryLolMirror(String mirrorUrl) {
        try {
            Document document = Jsoup.connect(mirrorUrl).get();

            Element downloadLinkElement = document.selectFirst("h2 > a[href]");
            if (downloadLinkElement != null) {
                String directDownloadLink = downloadLinkElement.attr("href");

                // Log the extracted direct download link
                System.out.println("Direct Download Link: " + directDownloadLink);

                downloadFile(directDownloadLink);
            } else {
                System.out.println("Direct download link not found on the mirror page.");
                openInBrowser(mirrorUrl);
            }
        } catch (HttpStatusException e) {
            handleHttpStatusException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String fileUrl) {
        try {
            // Replace backslashes with forward slashes in the URL
            String correctedUrl = fileUrl.replace("\\", "/");

            URI uri = new URI(correctedUrl);
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

            // Send the request and receive a response. If a redirect occurs, it is followed
            // automatically.
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Extract filename and extension from Content-Disposition header
            HttpHeaders headers = response.headers();
            String contentDisposition = headers.firstValue("Content-Disposition").orElse("");
            String filename = "";
            if (!contentDisposition.isEmpty()) {
                filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9);
                filename = filename.replaceAll("\"", ""); // Remove surrounding quotes if present
            }

            // If filename is not found in the header, generate a filename
            if (filename.isEmpty()) {
                filename = Paths.get(uri.getPath()).getFileName().toString();
            }

            // Sanitize the filename by replacing illegal characters
            filename = sanitizeFilename(filename);

            // Use the specified download directory
            Path outputPath = downloadDirectory.resolve(filename);

            // Copy the response body (file content) to the output path
            Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File downloaded successfully. Saved as: " + filename);
        } catch (HttpStatusException e) {
            handleHttpStatusException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleHttpStatusException(HttpStatusException e) {
        int statusCode = e.getStatusCode();
        System.out.println("File failed to download due to error: " + statusCode);
        String message;
        switch (statusCode) {
            case 404:
                message = "Error 404: File not found on the server.";
                break;
            case 403:
                message = "Error 403: Access forbidden. Bots may not be authorized.";
                break;
            case 502:
                message = "Error 502: Server got an invalid response.";
                break;
            default:
                message = "HTTP error " + statusCode + " occurred.";
        }
        JOptionPane.showMessageDialog(null, message, "Download Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void openInBrowser(String url) throws IOException {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(java.net.URI.create(url));
        } else {
            System.out.println("Desktop browsing not supported on this platform.");
        }
    }

    private static String sanitizeFilename(String filename) {
        // Replace or remove illegal characters based on your requirements
        // This is just a basic example; you may need to extend the replacement logic
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9._\\- ]");
        Matcher matcher = pattern.matcher(filename);
        return matcher.replaceAll("");
    }
}
