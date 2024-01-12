import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class Downloader {

    // Path to the directory where downloads will be saved
    private static Path downloadDirectory;

    // Sets the directory where downloaded files will be saved
    public static void setDownloadDirectory(Path directory) {
        downloadDirectory = directory;
    }

    // Method to download a file from a given mirror URL
    public static void downloadFromLibgenMirror(String mirrorUrl) {
        try {
            // Connects to the mirror URL and parses the HTML content
            Document document = Jsoup.connect(mirrorUrl).get();

            // Variable to store the direct download link
            String directDownloadLink = null;

            // Checks if the mirror URL is from 'library.lol' and extracts the download link
            if (mirrorUrl.contains("library.lol")) {
                Element downloadLinkElement = document.selectFirst("h2 > a[href]");
                if (downloadLinkElement != null) {
                    directDownloadLink = downloadLinkElement.attr("href");
                }
            } else {
                // Extracts the download link for other mirrors
                Element downloadLinkElement = document.selectFirst("td[bgcolor=#A9F5BC] a[href^=https://cdn]");
                if (downloadLinkElement != null) {
                    directDownloadLink = downloadLinkElement.attr("href");
                }
            }

            // Initiates the download process if the direct download link is found
            if (directDownloadLink != null) {
                System.out.println("Direct Download Link: " + directDownloadLink);
                downloadFile(directDownloadLink);
            } else {
                // Opens the mirror URL in a browser if the direct download link is not found
                System.out.println("Direct download link not found on the mirror page.");
                openInBrowser(mirrorUrl);
            }
        } catch (Exception e) {
            // Prints the stack trace for any exceptions encountered
            e.printStackTrace();
        }
    }

    // Similar to the above method but specifically for 'library.lol' mirror
    public static void downloadFromLibraryLolMirror(String mirrorUrl) {
        try {
            // Fetches and logs the document from the mirror URL
            Document document = Jsoup.connect(mirrorUrl).get();
            System.out.println("Fetched document: " + document);

            // Extracts and logs the direct download link
            Element downloadLinkElement = document.selectFirst("h2 > a[href]");
            if (downloadLinkElement != null) {
                String directDownloadLink = downloadLinkElement.attr("href");
                System.out.println("Direct Download Link: " + directDownloadLink);

                // Downloads the file from the extracted link
                downloadFile(directDownloadLink);
            } else {
                System.out.println("Direct download link not found on the mirror page.");
                openInBrowser(mirrorUrl);
            }
        } catch (HttpStatusException e) {
            // Handles specific HTTP status exceptions
            handleHttpStatusException(e);
        } catch (IOException e) {
            // Prints the stack trace for I/O exceptions
            e.printStackTrace();
        }
    }

    // Downloads the file from the provided URL
    private static void downloadFile(String fileUrl) {
        try {
            // Corrects the URL format if necessary
            String correctedUrl = fileUrl.replace("\\", "/");
            URI uri = new URI(correctedUrl);

            // Sets up the HTTP client with timeout settings
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // Creates an HTTP request with a response timeout
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMinutes(2))
                    .build();

            // Sends the request and receives the response stream
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Extracts the filename from the 'Content-Disposition' header
            HttpHeaders headers = response.headers();
            String contentDisposition = headers.firstValue("Content-Disposition").orElse("");
            String filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9);
            filename = filename.replaceAll("\"", "");

            // Sanitizes the filename and determines the output path
            filename = sanitizeFilename(filename);
            Path outputPath = downloadDirectory.resolve(filename);

            // Copies the file from the response stream to the output path
            Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File downloaded successfully. Saved as: " + filename);
        } catch (HttpStatusException e) {
            // Handles specific HTTP status exceptions
            handleHttpStatusException(e);
        } catch (IOException e) {
            // Logs I/O exception messages
            System.err.println("IO Exception occurred: " + e.getMessage());
        } catch (InterruptedException e) {
            // Handles interrupted exceptions
            Thread.currentThread().interrupt();
            System.err.println("Download interrupted: " + e.getMessage());
        } catch (Exception e) {
            // Logs unexpected exception messages
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Handles specific HTTP status exceptions with user-friendly messages
    private static void handleHttpStatusException(HttpStatusException e) {
        int statusCode = e.getStatusCode();
        String message;
        switch (statusCode) {
            case 404:
                message = "Error 404: File not found on the server.";
                break;
            case 403:
                message = "Error 403: Access forbidden. Bots may not be authorized.";
                break;
            default:
                message = "HTTP error " + statusCode + " occurred.";
        }
        JOptionPane.showMessageDialog(null, message, "Download Error", JOptionPane.ERROR_MESSAGE);
    }

    // Opens a given URL in the default web browser
    private static void openInBrowser(String url) throws IOException {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI.create(url));
        } else {
            System.out.println("Desktop browsing not supported on this platform.");
        }
    }

    // Sanitizes filenames by removing illegal characters
    private static String sanitizeFilename(String filename) {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9._\\- ]");
        Matcher matcher = pattern.matcher(filename);
        return matcher.replaceAll("");
    }
}
