import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Window;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Downloader {

    private static Path downloadDirectory; // Add this variable

    public static void setDownloadDirectory(Path directory) {
        downloadDirectory = directory;
    }

    public static void downloadFromLibgenMirror(String mirrorUrl, Set<String> ongoingDownloads) {
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
                    downloadFile(directDownloadLink, ongoingDownloads); // Download the file using Java's HTTP client
                } else {
                    System.out.println("Direct download link not found on the mirror page.");
                    openInBrowser(mirrorUrl);
                }
            } else {
                System.out.println("Non-HTML content, downloading file directly.");
                downloadFile(mirrorUrl, ongoingDownloads); // Directly download if the content is not HTML
            }
        } catch (HttpStatusException e) {
            System.out.println("Error fetching URL: " + e.getMessage());
            handleHttpStatusException(e, mirrorUrl);
        } catch (UnsupportedMimeTypeException e) {
            System.out.println("Direct file download link detected, bypassing HTML parsing.");
            downloadFile(mirrorUrl, ongoingDownloads);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFromLibraryLolMirror(String mirrorUrl, Set<String> ongoingDownloads) {
        try {
            Document document = Jsoup.connect(mirrorUrl).get();

            Element downloadLinkElement = document.selectFirst("h2 > a[href]");
            if (downloadLinkElement != null) {
                String directDownloadLink = downloadLinkElement.attr("href");

                // Log the extracted direct download link
                System.out.println("Direct Download Link: " + directDownloadLink);

                downloadFile(directDownloadLink, ongoingDownloads);
            } else {
                System.out.println("Direct download link not found on the mirror page.");
                openInBrowser(mirrorUrl);
            }
        } catch (HttpStatusException e) {
            handleHttpStatusException(e, mirrorUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String fileUrl, Set<String> ongoingDownloads) {
        String filename = ""; // Declare filename outside the try block
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

            // Add the filename to ongoingDownloads
            System.out.println("Adding download: " + filename + " to ongoingDownloads set");
            ongoingDownloads.add(filename);
            showDownloadStartedAlert(filename);

            int statusCode = response.statusCode();
            if (statusCode == 200) { // Check if the response status code is 200 (success)
                // Use the specified download directory
                Path outputPath = downloadDirectory.resolve(filename);

                // Copy the response body (file content) to the output path
                Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("File downloaded successfully. Saved as: " + filename);
            } else {
                // Handle HTTP status error
                throw new HttpStatusException("HTTP error fetching URL: " + fileUrl + ". Status=" + statusCode,
                        statusCode, correctedUrl);
            }
        } catch (HttpStatusException e) {
            System.out.println("Removing download from ongoingDownloads set");
            ongoingDownloads.remove(filename);
            handleHttpStatusException(e, fileUrl);
        } catch (IOException e) {
            System.out.println("Removing download from ongoingDownloads set due to IOException");
            ongoingDownloads.remove(filename);
            handleIOException(e, filename, fileUrl);
        } catch (Exception e) {
            System.out.println("Removing download from ongoingDownloads set due to Exception");
            ongoingDownloads.remove(filename);
            e.printStackTrace();
        } finally {
            // Remove the filename from ongoingDownloads in the finally block
            System.out.println("Removing download from ongoingDownloads set");
            ongoingDownloads.remove(filename);
        }
    }
        private static void showDownloadStartedAlert(String bookTitle) {
            StringBuilder alertMessage = new StringBuilder("Download started for: " + bookTitle + "\n");

            // Create an "OK" button
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                // Close the dialog when button is clicked
                Window window = SwingUtilities.getWindowAncestor(okButton);
                if (window instanceof JDialog) {
                    JDialog dialog = (JDialog) window;
                    dialog.dispose();
                }
            });

            // Create a JOptionPane with PLAIN_MESSAGE type and custom OK button
            JOptionPane pane = new JOptionPane(alertMessage.toString(), JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, new Object[] { okButton });

            // Create a JDialog from JOptionPane
            JDialog dialog = pane.createDialog("Download Started");

            // Load your custom icon
            ImageIcon icon = new ImageIcon(Downloader.class.getResource("icon.png"));
            dialog.setIconImage(icon.getImage());

            // Display the dialog
            dialog.setVisible(true);
        }
    private static void handleIOException(IOException e, String filename, String fileUrl) {
        String displayFilename = filename;
        if (filename.length() > 20) {
            displayFilename = filename.substring(0, 20) + "...";
        }

        String message;
        if (e.getMessage().contains("closed")) {
            // Handle the specific "closed" IOException
            message = "The download for '" + displayFilename + "' failed: Stream closed\nURL: " + fileUrl;
        } else {
            // General IOException handling
            message = "An error occurred during the download of '" + displayFilename + "': " + e.getMessage()
                    + "\nURL: " + fileUrl;
        }
        System.out.println("IOException for file " + filename + " (" + fileUrl + "): " + e.getMessage());

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setCaretPosition(0);
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(UIManager.getBorder("TextField.border"));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 150));

        JOptionPane.showMessageDialog(null, scrollPane, "Download Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void handleHttpStatusException(HttpStatusException e, String url) {
        int statusCode = e.getStatusCode();
        String errorMessage;
        switch (statusCode) {
            case 404:
                errorMessage = "Error 404: File not found on the server.\nURL: " + url;
                break;
            case 403:
                errorMessage = "Error 403: Access forbidden. Bots may not be authorized.\nURL: " + url;
                break;
            case 502:
                errorMessage = "Error 502: Server returned an invalid response.\nURL: " + url;
                break;
            default:
                errorMessage = "HTTP error " + statusCode + " occurred.\nURL: " + url;
        }

        // Create a JTextArea to display the error message
        JTextArea textArea = new JTextArea(errorMessage);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setCaretPosition(0);
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setBorder(UIManager.getBorder("TextField.border"));

        // Create a JScrollPane to allow scrolling if the message is too long
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 150)); // Set preferred size

        // Show the error message in a dialog
        int option = JOptionPane.showOptionDialog(null, scrollPane, "Download Error", JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE, null, null, null);

        if (option == JOptionPane.OK_OPTION) {
            // User clicked OK, you can add further handling here if needed
        }
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
