import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    private static JLabel pageCountLabel;
    private static boolean isSearchInProgress = false;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(LibGenSearchApp::createAndShowGUI);
    }
    private static void openLinkInBrowser(String finalMirror) {
        // Create a JTextArea to display the message
        JTextArea messageTextArea = new JTextArea(
            "Do you want to open the uploader in the browser?\n" + finalMirror
            + "\n\n" + "Username: genesis\nPassword: upload"
        );
        messageTextArea.setEditable(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setLineWrap(true);
        messageTextArea.setCaretPosition(0);
        messageTextArea.setBackground(UIManager.getColor("Label.background"));
        messageTextArea.setFont(UIManager.getFont("Label.font"));
        messageTextArea.setBorder(UIManager.getBorder("TextField.border"));
        messageTextArea.setPreferredSize(new Dimension(350, 100));
    
        // Create a JDialog for the custom dialog
        JDialog dialog = new JDialog();
        try {
            dialog.setIconImage(ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.setTitle("Open uploader");
        dialog.setModal(true); // Set the dialog to be modal
    
        // Create a panel to hold the components
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(messageTextArea, BorderLayout.CENTER);
    
        // Create a button to open the link
        JButton openButton = new JButton("Open Link");
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(finalMirror));
                // Optionally, close the dialog after opening the link
                // dialog.dispose();
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });
    
        // Create a button to close the dialog
        JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(e -> dialog.dispose());
    
        // Add buttons to the panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    
        // Add the panel to the dialog
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null); // Center the dialog
        dialog.setVisible(true); // Show the dialog
    }
    private static void createAndShowGUI() {
        frame = new JFrame("Simple Libgen Desktop");
        try {
            frame.setIconImage(ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 500);
    
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
        // Upload menu
        JMenu uploadMenu = new JMenu("Upload");
        JMenuItem fictionItem = new JMenuItem("Fiction");
        fictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/fiction/upload/"));
        uploadMenu.add(fictionItem);
        
        JMenuItem nonFictionItem = new JMenuItem("Non-Fiction");
        nonFictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/main/upload/"));
        uploadMenu.add(nonFictionItem);

        menuBar.add(uploadMenu);
        frame.setLayout(new BorderLayout()); // Set the layout of the frame
    
        panel = new JPanel();
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
    
        imagePanel = new JPanel();
        loadingStatusLabel = new JLabel("");
        Dimension labelSize = new Dimension(50, 20); // Set the desired size
        loadingStatusLabel.setPreferredSize(labelSize);
        loadingStatusLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center-align the text

        
        searchButton.addActionListener(e -> {
            currentPage = 1; // Reset to the first page on new search
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
        pageCountLabel = new JLabel("Page " + currentPage); // Initialize the page count label
        previousButton = new JButton("Previous");
        nextButton = new JButton("Next");
    
        // Initialize button states based on the initial content of searchField
        updateButtonStates();

        // Add action listeners to the buttons
        previousButton.addActionListener(e -> navigatePage(-1));
        nextButton.addActionListener(e -> navigatePage(1));
    
        // Add the buttons to the pagination panel
        paginationPanel.add(pageCountLabel); // Add the page count label here
        pageCountLabel.setVisible(false);
        paginationPanel.add(previousButton);
        paginationPanel.add(nextButton);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);
    
        // Add panels to the frame
        frame.add(panel, BorderLayout.CENTER); // Add the main panel to the center
        frame.add(paginationPanel, BorderLayout.SOUTH); // Add the pagination panel to the bottom
    
        frame.setVisible(true);
        
        // Add DocumentListener to the searchField
    searchField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            updateButtonStates();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateButtonStates();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateButtonStates();
        }
    });
    }
    private static void updateButtonStates() {
        boolean isSearchFieldEmpty = searchField.getText().trim().isEmpty();
        searchButton.setEnabled(!isSearchFieldEmpty && !isSearchInProgress); // Disable search button if the field is empty or a search is in progress
        
    }

    private static void navigatePage(int delta) {
        currentPage += delta;
        
        performSearch(); // Call performSearch to load the new page
    }

    private static void setLanguageCode() {
        // Create a text field for input
        JTextField inputField = new JTextField(10);
    
        // Create the buttons for the dialog
        JButton okButton = new JButton("OK");
        okButton.setEnabled(false); // Initially disabled
        JButton cancelButton = new JButton("Cancel");
    
        // Panel to hold the input field and label
        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter three-letter language code:"));
        panel.add(inputField);
    
        // Listener to enable OK button only when input is three letters
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = inputField.getText().trim();
                okButton.setEnabled(text.matches("[a-zA-Z]{3}"));
            }
    
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }
    
            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }
    
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
    
        // Create a JOptionPane
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{}, null);
    
        // Create a JDialog and set its button behavior
        final JDialog dialog = new JDialog(frame, "Set Language Code", true);
        dialog.setContentPane(optionPane);
    
        okButton.addActionListener(e -> {
            dialog.dispose();
            String newLanguageCode = inputField.getText().trim();
            if (newLanguageCode.matches("[a-zA-Z]{3}")) {
                languageCode = newLanguageCode.toLowerCase();
            }
        });
    
        cancelButton.addActionListener(e -> dialog.dispose());
    
        optionPane.setOptions(new Object[]{okButton, cancelButton});
    
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
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
        if (!userInput.isEmpty() && !isSearchInProgress) {
            showLoadingStatusLabel();
            isSearchInProgress = true;
            searchButton.setEnabled(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
    
            try {
                String encodedQuery = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
                String url = constructLibGenUrl(encodedQuery, currentPage);
    
                SwingWorker<List<ImageDetails>, Integer> worker = new SwingWorker<List<ImageDetails>, Integer>() {
                    @Override
                    protected List<ImageDetails> doInBackground() throws Exception {
                        return scrapeLibGenImages(url);
                    }
    
                    @Override
                    protected void done() {
                        try {
                            List<ImageDetails> imageDetailsList = get();
                    
                            imagePanel.removeAll(); // Clear previous results
                    
                            if (imageDetailsList.isEmpty()) {
                                // No results found
                                searchButton.setEnabled(true);
                                pageCountLabel.setVisible(false);
                                imagePanel.setVisible(false);
                                JOptionPane.showMessageDialog(frame, "No results found", "Search Results", JOptionPane.INFORMATION_MESSAGE);
                                nextButton.setEnabled(false); // Disable Next button if no results
                                boolean isSearchFieldEmpty = searchField.getText().trim().isEmpty();
                                previousButton.setEnabled(!isSearchFieldEmpty && currentPage > 1);
                            } else {
                                searchButton.setEnabled(true);
                                pageCountLabel.setText("Page " + currentPage); // Update the page count label
                                pageCountLabel.setVisible(true);
                                boolean isSearchFieldEmpty = searchField.getText().trim().isEmpty();
                                previousButton.setEnabled(!isSearchFieldEmpty && currentPage > 1);
                                nextButton.setEnabled(true);
                                imagePanel.setVisible(true);
                                int resultsPerPage = 5;
                                int startIndex = (currentPage - 1) * resultsPerPage;
                                int endIndex = Math.min(startIndex + resultsPerPage, imageDetailsList.size());
    
                                for (int i = startIndex; i < endIndex; i++) {
                                    ImageDetails imageDetails = imageDetailsList.get(i);
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
    
                                nextButton.setEnabled(true); // Enable Next button as there are results
                            }
                    
                            imagePanel.revalidate();
                            imagePanel.repaint();
                            hideLoadingStatusLabel();
                            isSearchInProgress = false;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            hideLoadingStatusLabel();
                            isSearchInProgress = false;
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
        loadingStatusLabel.setText("Loading...");
    }
    
    private static void hideLoadingStatusLabel() {
        loadingStatusLabel.setText("");
    }

    private static String constructLibGenUrl(String encodedQuery, int page) {
        // Estimate the initial capacity to avoid incremental capacity increase
        int estimatedSize = 200 + encodedQuery.length() + selectedFilters.size() * 10;
        StringBuilder urlBuilder = new StringBuilder(estimatedSize);
    
        urlBuilder.append("https://libgen.li/index.php?req=")
                  .append(encodedQuery)
                  .append("+lang%3A")
                  .append(languageCode)
                  .append("&columns[]=t&columns[]=a&columns[]=s&columns[]=y&columns[]=p&columns[]=i&objects[]=f&objects[]=e&objects[]=s&objects[]=a&objects[]=p&objects[]=w");
    
        // Use stream API for appending filters
        selectedFilters.stream().forEach(filter -> urlBuilder.append("&topics[]=").append(filter));
    
        urlBuilder.append("&res=25&covers=on&gmode=on&filesuns=all");
    
        return urlBuilder.toString();
    }

    private static List<ImageDetails> scrapeLibGenImages(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements rows = document.select("table > tbody > tr");

            return rows.stream().map(row -> {
                Elements imgElement = row.select("td a img[src]");
                if (imgElement.isEmpty()) return null;

                String title = getTextOrPlaceholder(row, "td:nth-child(2) b", "No title set by uploader");
                String author = getTextOrPlaceholder(row, "td:nth-child(3)", "No author set by uploader");
                String publisher = getTextOrPlaceholder(row, "td:nth-child(4)", "No publisher set by uploader");
                String year = getTextOrPlaceholder(row, "td:nth-child(5)", "No year set by uploader");
                String lang = getTextOrPlaceholder(row, "td:nth-child(6)", "No lang set by uploader");
                String size = getTextOrPlaceholder(row, "td:nth-child(8)", "No size calculated by libgen");
                List<String> mirrors = row.select("td:last-child a").stream()
                    .map(mirrorElement -> mirrorElement.attr("href"))
                    .collect(Collectors.toList());
                String imageUrl = imgElement.first().attr("abs:src").replace("_small", "");

                return new ImageDetails(imageUrl, title, author, publisher, year, lang, size, mirrors);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static String getTextOrPlaceholder(Element row, String cssQuery, String placeholder) {
        String text = row.select(cssQuery).text().trim();
        return text.isEmpty() ? placeholder : text;
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
        
            // Create a custom dialog
            JDialog dialog = new JDialog();
            dialog.setTitle("Select Download Directory");
            try {
                // Set custom icon for the dialog
                dialog.setIconImage(ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        
            dialog.add(fileChooser);
            dialog.pack();
            dialog.setLocationRelativeTo(null); // Center the dialog on screen
        
            // Show the dialog and get the user's selection
            int userSelection = fileChooser.showOpenDialog(dialog);
        
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