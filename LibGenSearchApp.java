import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
    private static JLabel loadingStatusLabel;
    private static Path downloadDirectory;

    private static String languageCode = "eng";
    private static List<String> selectedFilters = new ArrayList<>();

    private static int currentPage = 1; // New field to track the current page
    private static JButton previousButton; // New field for the previous page button
    private static JButton nextButton; // New field for the next page button

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(LibGenSearchApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        frame = new JFrame("LibGen Search App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 500);
    
        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem setLanguageItem = new JMenuItem("Set Language Code");
    
        setLanguageItem.addActionListener(e -> setLanguageCode());
    
        // Add Filter submenu to the Options menu
        JMenu filterMenu = new JMenu("Filter");
        addFilterCheckBox(filterMenu, "Libgen", "l");
        addFilterCheckBox(filterMenu, "Comics", "c");
        addFilterCheckBox(filterMenu, "Fiction", "f");
        addFilterCheckBox(filterMenu, "Scientific Articles", "a");
        addFilterCheckBox(filterMenu, "Magazines", "m");
        addFilterCheckBox(filterMenu, "Fiction RUS", "r");
        addFilterCheckBox(filterMenu, "Standards", "s");
    
        // Add menu items to the Options menu
        optionsMenu.add(setLanguageItem);
        optionsMenu.add(filterMenu);
    
        // Add the menu bar to the frame
        menuBar.add(optionsMenu);
        frame.setJMenuBar(menuBar);
    
        frame.setLayout(new BorderLayout()); // Set the layout of the frame
    
        panel = new JPanel();
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
    
        imagePanel = new JPanel();
        loadingStatusLabel = new JLabel("");
    
        searchButton.addActionListener(e -> {
            currentPage = 1; // Reset to the first page on new search
            updatePaginationButtons(); // Update the state of pagination buttons
            performSearch(); // Perform the search
        });
    
        panel.add(new JLabel("Enter search query: "));
        panel.add(searchField);
        panel.add(searchButton);
    
        imagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    
        // Add components to the main panel
        panel.add(loadingStatusLabel);
        panel.add(imagePanel);
    
        // Pagination panel setup
        JPanel paginationPanel = new JPanel(); // Create a panel for pagination
        previousButton = new JButton("Previous");
        nextButton = new JButton("Next");
    
        // Add action listeners to the buttons
        previousButton.addActionListener(e -> navigatePage(-1));
        nextButton.addActionListener(e -> navigatePage(1));
    
        // Add the buttons to the pagination panel
        paginationPanel.add(previousButton);
        paginationPanel.add(nextButton);
    
        // Add panels to the frame
        frame.add(panel, BorderLayout.CENTER); // Add the main panel to the center
        frame.add(paginationPanel, BorderLayout.SOUTH); // Add the pagination panel to the bottom
    
        frame.setVisible(true);
        initializePagination();

    }
    private static void initializePagination() {
        updatePaginationButtons(); // Set the initial state of the buttons
    }
    private static void navigatePage(int delta) {
        currentPage += delta;
        updatePaginationButtons(); // Update the state of pagination buttons
        performSearch(); // Call performSearch to load the new page
    }
    private static void updatePaginationButtons() {
        // Disable the Previous button on the first page
        previousButton.setEnabled(currentPage > 1);
    
        // Future Implementation: Disable the Next button if there are no more pages.
        // This requires knowledge of the total number of pages or results.
        // nextButton.setEnabled(currentPage < totalPages);
    }
    private static void setLanguageCode() {
        String newLanguageCode = JOptionPane.showInputDialog(frame, "Enter three-letter language code:");
        if (newLanguageCode != null && newLanguageCode.length() == 3) {
            languageCode = newLanguageCode.toLowerCase();
        } else {
            JOptionPane.showMessageDialog(frame, "Invalid language code. Please enter a three-letter code.");
        }
    }

    private static void addFilterCheckBox(JMenu filterMenu, String label, String filterValue) {
        JCheckBoxMenuItem filterItem = new JCheckBoxMenuItem(label);
        filterItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);
        filterItem.setSelected(true);
        filterItem.addActionListener(e -> handleFilterSelection(filterItem, filterValue));
        
        filterMenu.add(filterItem);
    }

    private static void handleFilterSelection(JCheckBoxMenuItem filterItem, String filterValue) {
        if (filterItem.isSelected()) {
            selectedFilters.add(filterValue);
        } else {
            selectedFilters.remove(filterValue);
        }
    }

    private static void performSearch() {
        String userInput = searchField.getText().trim();
        if (!userInput.isEmpty()) {
            showLoadingStatusLabel();

            try {
                String encodedQuery = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
                String url = constructLibGenUrl(encodedQuery, currentPage);

                SwingWorker<List<ImageDetails>, Integer> worker = new SwingWorker<>() {
                    @Override
                    protected List<ImageDetails> doInBackground() throws Exception {
                        List<ImageDetails> imageDetailsList = scrapeLibGenImages(url);
                        int totalImages = imageDetailsList.size();

                        for (int i = 0; i < totalImages; i++) {
                            publish(i);
                            Thread.sleep(100);
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
                    
                            // Define the number of results per page
                            int resultsPerPage = 5;
                    
                            // Calculate the start index for the current page
                            int startIndex = (currentPage - 1) * resultsPerPage;
                    
                            // Calculate the end index, ensuring it does not exceed the list size
                            int endIndex = Math.min(startIndex + resultsPerPage, imageDetailsList.size());
                    
                            // Clear the previous results from the panel
                            imagePanel.removeAll();
                    
                            // Iterate over the subset of results for the current page
                            for (int i = startIndex; i < endIndex; i++) {
                                ImageDetails imageDetails = imageDetailsList.get(i);
                    
                                // Process and display each image as before
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
                    
                            // Refresh the panel to display the new results
                            imagePanel.revalidate();
                            imagePanel.repaint();
                    
                            // Hide the loading status label
                            hideLoadingStatusLabel();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            hideLoadingStatusLabel();
                        }
                    }
                };

                worker.execute();
            } catch (IOException ex) {
                ex.printStackTrace();
                hideLoadingStatusLabel();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Please enter a search query.");
        }
    }

    private static void showLoadingStatusLabel() {
        loadingStatusLabel.setText("Loaded 0 items");
    }

    private static void updateLoadingStatusLabel(int progress) {
        if (progress >= 24) {
            loadingStatusLabel.setText("Rendering...");
        } else {
            loadingStatusLabel.setText("Loaded " + (progress + 1) + " items");
        }
    }

    private static void hideLoadingStatusLabel() {
        loadingStatusLabel.setText("");
    }

    private static String constructLibGenUrl(String encodedQuery, int page) {
        StringBuilder urlBuilder = new StringBuilder("https://libgen.li/index.php?req=");
        urlBuilder.append(encodedQuery)
                  .append("+lang%3A")
                  .append(languageCode)
                  .append("&columns[]=t&columns[]=a&columns[]=s&columns[]=y&columns[]=p&columns[]=i&objects[]=f&objects[]=e&objects[]=s&objects[]=a&objects[]=p&objects[]=w");
    
        for (String filter : selectedFilters) {
            urlBuilder.append("&topics[]=").append(filter);
        }
    
        urlBuilder.append("&res=25&covers=on&gmode=on&filesuns=all");
    
        return urlBuilder.toString();
    }

    private static List<ImageDetails> scrapeLibGenImages(String url) {
        List<ImageDetails> imageDetailsList = new ArrayList<>();
        try {
            Document document = Jsoup.connect(url).get();
            Elements rows = document.select("table > tbody > tr");

            for (Element row : rows) {
                Elements imgElement = row.select("td a img[src]");
                if (imgElement.isEmpty()) {
                    continue;
                }

                String title = row.select("td:nth-child(2) b").text().trim();
                if (title.isEmpty()) {
                    title = "No title set by uploader";
                }
                String author = row.select("td:nth-child(3)").text().trim();
                if (author.isEmpty()) {
                    author = "No author set by uploader";
                }
                String publisher = row.select("td:nth-child(4)").text().trim();
                if (publisher.isEmpty()) {
                    publisher = "No publisher set by uploader";
                }
                String year = row.select("td:nth-child(5)").text().trim();
                if (year.isEmpty()) {
                    year = "No year set by uploader";
                }
                String lang = row.select("td:nth-child(6)").text().trim();
                if (lang.isEmpty()) {
                    lang = "No lang set by uploader";
                }
                String size = row.select("td:nth-child(8)").text().trim();
                if (size.isEmpty()) {
                    size = "No size calculated by libgen";
                }
                Elements mirrorsElements = row.select("td:last-child a");
                List<String> mirrors = new ArrayList<>();
                for (Element mirrorElement : mirrorsElements) {
                    mirrors.add(mirrorElement.attr("href"));
                }

                String imageUrl = imgElement.first().attr("abs:src").replace("_small", "");

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
            String detailsMessage = "Title: " + imageDetails.getTitle() + "\n" +
                    "Author: " + imageDetails.getAuthor() + "\n" +
                    "Publisher: " + imageDetails.getPublisher() + "\n" +
                    "Year: " + imageDetails.getYear() + "\n" +
                    "Language: " + imageDetails.getLang() + "\n" +
                    "Size: " + imageDetails.getSize();
    
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
    
            for (String mirror : imageDetails.getMirrors()) {
                if (!mirror.startsWith("http")) {
                    mirror = "https://libgen.li" + mirror;
                }
    
                JButton mirrorButton = new JButton(getBaseURL(mirror));
                final String finalMirror = mirror;
    
                mirrorButton.addActionListener(event -> {
                    if (finalMirror.contains("library.lol") || finalMirror.contains("libgen.li")) {
                        handleLibgenMirrorButtonClick(finalMirror);
                    } else {
                        openLinkInBrowser(finalMirror);
                    }
                });
    
                buttonsPanel.add(mirrorButton);
            }
    
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BorderLayout());
            detailsPanel.add(new JTextArea(detailsMessage), BorderLayout.CENTER);
            detailsPanel.add(buttonsPanel, BorderLayout.SOUTH);
    
            JOptionPane.showMessageDialog(null, detailsPanel, "Image Details", JOptionPane.INFORMATION_MESSAGE);
        }

        private String getBaseURL(String mirrorLink) {
            int slashIndex = mirrorLink.indexOf("/", 8);
            if (slashIndex != -1) {
                return mirrorLink.substring(0, slashIndex);
            } else {
                return mirrorLink;
            }
        }
        private void handleLibgenMirrorButtonClick(String finalMirror) {
            System.out.println("Mirror Link Clicked: " + finalMirror);
        
            if (selectDownloadDirectory()) {
                showDownloadStartedAlert(imageDetails.getTitle());
        
                Thread downloadThread = new Thread(() -> {
                    Downloader.setDownloadDirectory(downloadDirectory);
        
                    if (downloadDirectory != null) {
                        if (finalMirror.contains("library.lol")) {
                            System.out.println("Downloading from library.lol mirror");
                            Downloader.downloadFromLibraryLolMirror(finalMirror);
                        } else {
                            System.out.println("Downloading from other mirror");
                            Downloader.downloadFromLibgenMirror(finalMirror);
                        }
                    } else {
                        System.out.println("Download canceled: No directory selected.");
                    }
                });
        
                downloadThread.start();
            }
        }

        private void openLinkInBrowser(String finalMirror) {
            try {
                Desktop.getDesktop().browse(new URI(finalMirror));
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }

        private void showDownloadStartedAlert(String bookTitle) {
            StringBuilder alertMessage = new StringBuilder("Download started for: " + bookTitle + "\n");
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