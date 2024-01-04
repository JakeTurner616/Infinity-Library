

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.event.MouseInputAdapter;

public class LibGenSearchApp {



    private static JFrame frame;
    private static JPanel panel;
    private static JTextField searchField;
    private static JButton searchButton;
    private static JPanel imagePanel;
    private static JLabel loadingStatusLabel; // Changed to a JLabel
    private static Path downloadDirectory;

    public static void main(String[] args) {
        // Set the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        frame = new JFrame("LibGen Search App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 500);


        panel = new JPanel();
        searchField = new JTextField(20);
        searchButton = new JButton("Search");

        imagePanel = new JPanel();
        loadingStatusLabel = new JLabel(""); // Initialize with default text

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput = searchField.getText().trim();
                if (!userInput.isEmpty()) {
                    // Show loading status label
                    showLoadingStatusLabel();

                    try {
                        String encodedQuery = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
                        String url = constructLibGenUrl(encodedQuery);

                        SwingWorker<List<ImageDetails>, Integer> worker = new SwingWorker<>() {
                            @Override
                            protected List<ImageDetails> doInBackground() throws Exception {
                                List<ImageDetails> imageDetailsList = scrapeLibGenImages(url);
                                int totalImages = imageDetailsList.size();

                                for (int i = 0; i < totalImages; i++) {
                                    publish(i); // Publish progress
                                    Thread.sleep(100); // Simulate some processing time
                                }

                                return imageDetailsList;
                            }

                            @Override
                            protected void process(List<Integer> chunks) {
                                int progress = chunks.get(chunks.size() - 1);
                                updateLoadingStatusLabel(progress);
                            }

                            @Override
                            protected void done() {
                                try {
                                    List<ImageDetails> imageDetailsList = get();

                                    // Clear existing images
                                    imagePanel.removeAll();

                                    for (int i = 0; i < Math.min(imageDetailsList.size(), 5); i++) {
                                        ImageDetails imageDetails = imageDetailsList.get(i);
                                        System.out.println("Image URL: " + imageDetails.getImageUrl());

                                        ImageIcon originalIcon = new ImageIcon(new java.net.URI(imageDetails.getImageUrl()).toURL());
                                        int maxWidth = 229;
                                        int maxHeight = 327;

                                        int originalWidth = originalIcon.getIconWidth();
                                        int originalHeight = originalIcon.getIconHeight();

                                        int scaledWidth, scaledHeight;

                                        if (originalWidth > originalHeight) {
                                            scaledWidth = Math.min(originalWidth, maxWidth);
                                            scaledHeight = (int) (originalHeight * (double) scaledWidth / originalWidth);
                                        } else {
                                            scaledHeight = Math.min(originalHeight, maxHeight);
                                            scaledWidth = (int) (originalWidth * (double) scaledHeight / originalHeight);
                                        }

                                        Image scaledImage = originalIcon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                                        ImageIcon scaledIcon = new ImageIcon(scaledImage);

                                        JLabel imageLabel = new JLabel(scaledIcon);
                                        imageLabel.addMouseListener(new ImageClickListener(imageDetails));
                                        imagePanel.add(imageLabel);
                                    }

                                    // Refresh the UI
                                    imagePanel.revalidate();
                                    imagePanel.repaint();

                                    // Hide loading status label
                                    hideLoadingStatusLabel();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        };

                        worker.execute();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        // Hide loading status label in case of an exception
                        hideLoadingStatusLabel();
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Please enter a search query.");
                }
            }
        });

        panel.add(new JLabel("Enter search query: "));
        panel.add(searchField);
        panel.add(searchButton);

        imagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Add components to the main panel
        panel.add(loadingStatusLabel);
        panel.add(imagePanel);

        frame.getContentPane().add(panel);
        //frame.pack();
        frame.setVisible(true);
    }

    private static void showLoadingStatusLabel() {
        // Show the loading status label
        loadingStatusLabel.setText("Loaded 0 items");
    }

    private static void updateLoadingStatusLabel(int progress) {
        // Update the loading status label based on progress
        if (progress >= 24) { // Check if the counter is about to hit 25 (0-based index)
            loadingStatusLabel.setText("Rendering...");
        } else {
            loadingStatusLabel.setText("Loaded " + (progress + 1) + " items");
        }
    }

    private static void hideLoadingStatusLabel() {
        // Hide the loading status label
        loadingStatusLabel.setText("");
    }

    private static String constructLibGenUrl(String encodedQuery) {
        return "https://libgen.li/index.php?req=" + encodedQuery + "+lang%3Aeng&columns[]=t&columns[]=a&columns[]=s&columns[]=y&columns[]=p&columns[]=i&objects[]=f&objects[]=e&objects[]=s&objects[]=a&objects[]=p&objects[]=w&topics[]=l&topics[]=c&topics[]=f&topics[]=a&topics[]=m&topics[]=r&topics[]=s&res=25&covers=on&gmode=on&filesuns=all";
    }

    private static List<ImageDetails> scrapeLibGenImages(String url) {
        List<ImageDetails> imageDetailsList = new ArrayList<>();
        try {
            Document document = Jsoup.connect(url).get();
            Elements rows = document.select("table > tbody > tr");

            for (Element row : rows) {
                Elements imgElement = row.select("td a img[src]");
                if (imgElement.isEmpty()) {
                    continue; // Skip rows without images
                }

                // Extracting details from different columns
                String title = row.select("td:nth-child(2) b").text().trim();
                String author = row.select("td:nth-child(3)").text().trim();
                String publisher = row.select("td:nth-child(4)").text().trim();
                String year = row.select("td:nth-child(5)").text().trim();
                String lang = row.select("td:nth-child(6)").text().trim();
                String size = row.select("td:nth-child(8)").text().trim();

                // Extracting mirrors from the last <td> element
                Elements mirrorsElements = row.select("td:last-child a");
                List<String> mirrors = new ArrayList<>();
                for (Element mirrorElement : mirrorsElements) {
                    mirrors.add(mirrorElement.attr("href"));
                }

                String imageUrl = imgElement.first().attr("abs:src").replace("_small", "");

                // Creating an ImageDetails object with the extracted details
                ImageDetails imageDetails = new ImageDetails(imageUrl, title, author, publisher, year, lang, size, mirrors);
                imageDetailsList.add(imageDetails);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageDetailsList;
    }

    private static class ImageClickListener extends MouseInputAdapter {
        private final ImageDetails imageDetails;
    
        public ImageClickListener(ImageDetails imageDetails) {
            this.imageDetails = imageDetails;
        }
    
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
            // Display image details in a JOptionPane (you can customize this part)
            String detailsMessage = "Title: " + imageDetails.getTitle() + "\n" +
                    "Author: " + imageDetails.getAuthor() + "\n" +
                    "Publisher: " + imageDetails.getPublisher() + "\n" +
                    "Year: " + imageDetails.getYear() + "\n" +
                    "Language: " + imageDetails.getLang() + "\n" +
                    "Size: " + imageDetails.getSize();
    
            // Create a panel for the buttons
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
    
            // Adding buttons for each mirror link
            for (String mirror : imageDetails.getMirrors()) {
                // Ensure the mirror link has a base URL (https://libgen.li)
                if (!mirror.startsWith("http")) {
                    mirror = "https://libgen.li" + mirror;
                }
    
                // Extract the base URL from the mirror link
                String baseURL = getBaseURL(mirror);
    
                JButton mirrorButton = new JButton(baseURL);  // Set the base URL as the button label
                final String finalMirror = mirror; // To use in the lambda expression
    
                if ("https://libgen.li".equals(baseURL)) {
                    // Only for libgen.li mirror: Open directory selection and display alert
                    mirrorButton.addActionListener((event) -> {
                        // Print the mirror link in the terminal
                        System.out.println("Mirror Link: " + finalMirror);
    
                        // Prompt the user to select a download directory
                        if (selectDownloadDirectory()) {
                            // Display an alert box with download started message
                            showDownloadStartedAlert(imageDetails.getTitle());
    
                            // Use a separate thread for each download
                            Thread downloadThread = new Thread(() -> {
                                // Set the download directory in the Downloader class
                                Downloader.setDownloadDirectory(downloadDirectory);
    
                                // Check if the download directory is not null before proceeding
                                if (downloadDirectory != null) {
                                    // Download the file using the Downloader class
                                    Downloader.downloadFromLibgenMirror(finalMirror);
                                } else {
                                    // Handle the case where the user canceled the directory selection
                                    System.out.println("Download canceled: No directory selected.");
                                }
                            });
    
                            downloadThread.start();
                        }
                    });
                } else {
                    // For other mirrors: Open the link in the browser
                    mirrorButton.addActionListener((event) -> {
                        try {
                            // Open the mirror link in the default web browser
                            Desktop.getDesktop().browse(new URI(finalMirror));
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
    
                buttonsPanel.add(mirrorButton);
            }
    
            // Add the buttons panel to the details panel
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BorderLayout());
            detailsPanel.add(new JTextArea(detailsMessage), BorderLayout.CENTER);
            detailsPanel.add(buttonsPanel, BorderLayout.SOUTH);
    
            // Show the JOptionPane with the details and buttons
            JOptionPane.showMessageDialog(null, detailsPanel, "Image Details", JOptionPane.INFORMATION_MESSAGE);
        }
    
        private String getBaseURL(String mirrorLink) {
            // Extract the base URL from the mirror link
            int slashIndex = mirrorLink.indexOf("/", 8); // Find the first slash after "https://"
            if (slashIndex != -1) {
                return mirrorLink.substring(0, slashIndex);
            } else {
                return mirrorLink;
            }
        }
    
        private void showDownloadStartedAlert(String bookTitle) {
            // Build the message
            StringBuilder alertMessage = new StringBuilder("Download started for: " + bookTitle + "\n");
    
            // Display the alert box
            JOptionPane.showMessageDialog(null, alertMessage.toString(), "Download Started", JOptionPane.INFORMATION_MESSAGE);
        }
    
        private boolean selectDownloadDirectory() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Download Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    
            int userSelection = fileChooser.showOpenDialog(null);
    
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                downloadDirectory = fileChooser.getSelectedFile().toPath();
                System.out.println("Download location set to: " + downloadDirectory);
                return true;
            } else {
                // User canceled the directory selection
                System.out.println("Download directory selection canceled.");
                return false;
            }
        }
    }

    private static class ImageDetails {
        private final String imageUrl;
        private final String title;
        private final String author;
        private final String publisher;
        private final String year;
        private final String lang;
        private final String size;
        private final List<String> mirrors;
        

        public ImageDetails(String imageUrl, String title, String author, String publisher, String year, String lang, String size, List<String> mirrors) {
            this.imageUrl = imageUrl;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.year = year;
            this.lang = lang;
            this.size = size;
            this.mirrors = mirrors;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getPublisher() {
            return publisher;
        }

        public String getYear() {
            return year;
        }

        public String getLang() {
            return lang;
        }

        public String getSize() {
            return size;
        }

        public List<String> getMirrors() {
            return mirrors;
        }
    }
}