import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


import org.json.JSONObject;
import org.json.JSONArray;



public class Infinitylibrary {

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
    private static final int RESULTS_PER_PAGE = 5;
    private static Preferences prefs = Preferences.userRoot().node("org/serverboi/libgensearchapp");
    private static Set<String> ongoingDownloads = Collections.synchronizedSet(new HashSet<>()); // For tracking download
                                                                                                // status
    private static List<String> selectedFileTypes = new ArrayList<>();
    private static String currentMirrorUrl;

    public static void main(String[] args) {
        System.setProperty("sun.java2d.noddraw", Boolean.TRUE.toString());
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        languageCode = prefs.get("languageCode", "eng"); // Default lang to "eng" if not set

        SwingUtilities.invokeLater(Infinitylibrary::createAndShowGUI);
        // Load the download directory from preferences
        String savedDirPath = prefs.get("downloadDirectory", null);
        if (savedDirPath != null && !savedDirPath.isEmpty()) {
            Path savedDir = Paths.get(savedDirPath);
            if (Files.exists(savedDir) && Files.isDirectory(savedDir)) {
                downloadDirectory = savedDir;
            } else {
                System.out.println("Previously set download directory is no longer valid.");
                // Optionally prompt the user to select a new directory
            }
        }

        loadMirrorUrlFromPreferences();

    }

    private static SwingWorker<List<String>, Void> fetchBookTitles(String query) {
        return new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                System.out.println("Fetching autocomplete data");
                List<String> titles = new ArrayList<>();
                HttpClient client = HttpClient.newHttpClient();
                String urlString = "https://openlibrary.org/search.json?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=5" + "&lang=eng";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .build();
    
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray docs = jsonResponse.optJSONArray("docs");
                    if (docs != null) {
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.optJSONObject(i);
                            if (doc != null) {
                                String title = doc.optString("title", "Unknown Title");
                                titles.add(title);
                            }
                        }
                    }
                } else {
                    System.out.println("Failed to fetch data: HTTP status " + response.statusCode());
                }
                return titles;
            }
        };
    }


    private static void handleWindowClosing() {
        System.out.println("Window closing event triggered");
        if (!ongoingDownloads.isEmpty()) {
            String message = "The following downloads are still in progress:\n" +
                    String.join("\n", ongoingDownloads) +
                    "\n\nDo you want to exit and cancel these downloads?";
            int choice = JOptionPane.showConfirmDialog(frame, message, "Confirm Exit",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                System.exit(0);
            }
        } else {
            System.out.println("No ongoingDownloads calling system exit...");
            System.exit(0);
        }
    }

    private static void openLinkInBrowser(String finalMirror) {
        // Create a JTextArea to display the message
        JTextArea messageTextArea = new JTextArea(
                "Do you want to open the uploader in the browser?\n" + finalMirror
                        + "\n\n" + "Username: genesis\nPassword: upload");
        messageTextArea.setEditable(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setLineWrap(true);
        messageTextArea.setCaretPosition(0);
        messageTextArea.setBackground(UIManager.getColor("Label.background"));
        messageTextArea.setFont(UIManager.getFont("Label.font"));
        messageTextArea.setBorder(UIManager.getBorder("TextField.border"));

        // Wrap the JTextArea in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(messageTextArea);
        scrollPane.setPreferredSize(new Dimension(350, 100)); // Set the preferred size for the scroll pane

        // Create a JDialog for the custom dialog
        JDialog dialog = new JDialog();
        try {
            dialog.setIconImage(
                    ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.setTitle("Open uploader");
        dialog.setModal(true); // Set the dialog to be modal

        // Create a panel to hold the components
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER); // Add the scroll pane instead of the text area

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

        frame = new JFrame("Infinity Library");
        try {
            frame.setIconImage(
                    ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Get the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();

        // Set the frame size as a fraction of the screen size
        frame.setSize((int) (width * 0.625), (int) (height * 0.50)); // 62.5% of screen width and 50% of screen height

        // Get the current screen resolution
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        // Proportions for width and height
        double widthProportion = 0.15;
        double heightProportion = 0.2;

        // Calculate the desired width and height
        int desiredWidth = (int) (screenWidth * widthProportion);
        int desiredHeight = (int) (screenHeight * heightProportion);

        // Set the minimum size for the frame
        frame.setMinimumSize(new Dimension(desiredWidth, desiredHeight));

        JMenuBar menuBar = new JMenuBar();
        
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem setLanguageItem = new JMenuItem("Set Language Code");
        setLanguageItem.addActionListener(e -> setLanguageCode());
        JMenuItem defaultStorageLocation = new JMenuItem("Set Default Storage Location");

        defaultStorageLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Download Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Create a custom dialog
            JDialog dialog = new JDialog();
            dialog.setTitle("Select Download Directory");
            try {
                // Set custom icon for the dialog
                dialog.setIconImage(
                        ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            dialog.add(fileChooser);
            dialog.pack();
            dialog.setLocationRelativeTo(null); // Center the dialog on screen

            // Show the dialog and get the user's selection
            int userSelection = fileChooser.showOpenDialog(dialog);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                downloadDirectory = fileChooser.getSelectedFile().toPath();
                System.out.println("Download location set to: " + downloadDirectory);
                Preferences prefs = Preferences.userRoot().node("org/serverboi/infinitylibrary");
                prefs.put("downloadDirectory", downloadDirectory.toString());
            } else {
                System.out.println("Download directory selection canceled.");
                // Clear the stored preference if the user cancels the selection
                prefs.remove("downloadDirectory");
                downloadDirectory = null; // Reset the downloadDirectory variable as well
            }
        });

        JMenuItem mirrorSelectionItem = new JMenuItem("Set Upstream Mirror");
        mirrorSelectionItem.addActionListener(e -> showMirrorSelectionDialog());
        optionsMenu.add(defaultStorageLocation);
        optionsMenu.add(setLanguageItem);
        optionsMenu.add(mirrorSelectionItem);

        // Main Filters Menu
        JMenu filtersMenu = new JMenu("Filters");

        // Category Filters Submenu
        JMenu categoryMenu = new JMenu("Category");
        addFilterCheckBox(categoryMenu, "Libgen", "l");
        addFilterCheckBox(categoryMenu, "Comics", "c");
        addFilterCheckBox(categoryMenu, "Fiction", "f");
        addFilterCheckBox(categoryMenu, "Scientific Articles", "a");
        addFilterCheckBox(categoryMenu, "Magazines", "m");
        addFilterCheckBox(categoryMenu, "Fiction RUS", "r");
        addFilterCheckBox(categoryMenu, "Standards", "s");
        filtersMenu.add(categoryMenu);

        // Filetype Filters Submenu
        JMenu filetypeMenu = new JMenu("Filetype");
        createFileTypeDropdown(filetypeMenu);
        filtersMenu.add(filetypeMenu);

        optionsMenu.add(filtersMenu);
        menuBar.add(optionsMenu);

        loadFiltersFromPreferences(categoryMenu, filetypeMenu);

        JMenu uploadMenu = new JMenu("Upload");
        JMenuItem fictionItem = new JMenuItem("Fiction");
        fictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/fiction/upload/"));
        uploadMenu.add(fictionItem);
        JMenuItem nonFictionItem = new JMenuItem("Non-Fiction");
        nonFictionItem.addActionListener(e -> openLinkInBrowser("https://library.bz/main/upload/"));
        uploadMenu.add(nonFictionItem);
        menuBar.add(uploadMenu);
        
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem audiobookSearchItem = new JMenuItem("Audiobook Search");
        audiobookSearchItem.addActionListener(e -> Torrent.openAudiobookSearchDialog());
        toolsMenu.add(audiobookSearchItem);
        menuBar.add(toolsMenu);
        
        frame.setJMenuBar(menuBar);
        

        frame.setLayout(new BorderLayout());
        panel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Define and add the label
        // Define and add the label
        JLabel searchLabel = new JLabel("Enter search query: ");
        gbc.gridx = 0; // First column
        gbc.gridy = 0; // First row
        gbc.insets = new Insets(5, 5, 5, 5); // Padding
        gbc.anchor = GridBagConstraints.LINE_START;
        searchPanel.add(searchLabel, gbc);

        // Define and add the combo box for searching
        JComboBox<String> searchComboBox = new JComboBox<>(new DefaultComboBoxModel<>());
        searchComboBox.setEditable(true);

        // Hide the dropdown arrow button of the JComboBox
        Component[] components = searchComboBox.getComponents();
        for (Component component : components) {
            if (component instanceof JButton) {
                component.setVisible(false);
            }
        }

        searchField = (JTextField) searchComboBox.getEditor().getEditorComponent();
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchComboBox.hidePopup();
                    performSearch(); // Assuming you have a method to perform the search
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateSearchComboBox();
            }
            public void removeUpdate(DocumentEvent e) {
                updateSearchComboBox();
            }
            
            public void insertUpdate(DocumentEvent e) {
                updateSearchComboBox();
            }

            private boolean isUpdating = false; // Flag to indicate update is in progress
            private String lastFetchedText = ""; // To keep track of the last fetched text
            private String currentInputText = ""; // To store current text being typed

            private void updateComboBoxModel(List<String> titles) {
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) searchComboBox.getModel();
                model.removeAllElements();
                titles.forEach(model::addElement);
                restoreUserInput(); // Restore user input after updating the model
            }

            private void restoreUserInput() {
                searchField.setText(currentInputText); // Restore the text being typed by the user
            }

            private long lastRequestTime = 0;
            private static final long REQUEST_DELAY = 500; // Delay in milliseconds
            
            private void updateSearchComboBox() {
                SwingUtilities.invokeLater(() -> {
                    currentInputText = searchField.getText(); // Save current user input
            
                    if (!isUpdating && currentInputText.length() >= 1 && !currentInputText.equals(lastFetchedText)) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastRequestTime >= REQUEST_DELAY) {
                            System.out.println("Current text in searchField: " + currentInputText);
                            isUpdating = true;
                            lastFetchedText = currentInputText;
            
                            lastRequestTime = currentTime; // Update the last request time
            
                            SwingWorker<List<String>, Void> worker = fetchBookTitles(currentInputText);
                            worker.execute();
            
                            worker.addPropertyChangeListener(evt -> {
                                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                                    try {
                                        List<String> titles = worker.get();
                                        if (!titles.isEmpty()) {
                                            updateComboBoxModel(titles);
                                            searchComboBox.showPopup();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        isUpdating = false;
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

        gbc.gridx = 1; // Second column
        gbc.fill = GridBagConstraints.HORIZONTAL; // Allow horizontal resizing
        gbc.weightx = 1; // Give extra horizontal space to the combo box
        searchPanel.add(searchComboBox, gbc);

        // Define and add the search button
        searchButton = new JButton("Search");
        gbc.gridx = 2; // Third column
        gbc.fill = GridBagConstraints.NONE; // No resizing
        gbc.weightx = 0; // Reset extra horizontal space
        searchPanel.add(searchButton, gbc);

        JPanel containerPanel = new JPanel(new GridBagLayout());
        imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JScrollPane scrollableImagePanel = new JScrollPane(imagePanel);
        scrollableImagePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableImagePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        containerPanel.add(scrollableImagePanel, gbc);

        loadingStatusLabel = new JLabel("");
        Dimension labelSize = new Dimension(50, 30);
        loadingStatusLabel.setPreferredSize(labelSize);
        loadingStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        searchButton.addActionListener(e -> {
            searchComboBox.hidePopup(); // Hide the combo box popup
            currentPage = 1;
            performSearch(); // Call your search method
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(containerPanel, BorderLayout.CENTER);
        panel.add(loadingStatusLabel, BorderLayout.SOUTH);

        JPanel paginationPanel = new JPanel();
        pageCountLabel = new JLabel("Page " + currentPage);
        previousButton = new JButton("Previous");
        nextButton = new JButton("Next");
        previousButton.addActionListener(e -> navigatePage(-1));
        nextButton.addActionListener(e -> navigatePage(1));
        paginationPanel.add(pageCountLabel);
        pageCountLabel.setVisible(false);
        paginationPanel.add(previousButton);
        paginationPanel.add(nextButton);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(paginationPanel, BorderLayout.SOUTH);

        frame.setVisible(true);


        updateFilterCheckBoxes(filtersMenu, filetypeMenu);
    }

    /**
     * Removes unsupported file types from the user's saved preferences.
     * This method is invoked to ensure that the user's saved file type preferences
     * do not include extensions that are no longer supported by the application.
     * Specifically, it filters out 'cb7', 'azw4', and 'azw3' file types if they are present on disk.
     * 
     * Once the filtering process is complete, the method saves the updated
     * file type preferences back to the Preferences storage, maintaining the integrity
     * of the application's configuration and preventing any future use of deprecated
     * file formats. This allows us to control the supported file types and ensure that
     * the application remains functional and up-to-date with the latest file formats
     * supported by the upstream mirrors.
     */
    private static void removeUnsupportedFileTypes() {
        // Unsupported file types
        List<String> unsupportedFileTypes = Arrays.asList("cb7", "azw4", "azw3");
    
        // Filter out the unsupported file types
        selectedFileTypes.removeIf(unsupportedFileTypes::contains);
    
        // Save the updated file types back to preferences
        saveFiltersToPreferences();
    }
    
    private static void loadFiltersFromPreferences(JMenu categoryMenu, JMenu filetypeMenu) {
        String savedCategoryFilters = prefs.get("selectedCategoryFilters", "");
        String savedFileTypeFilters = prefs.get("selectedFileTypeFilters", "");
    
        // Load category filters
        if (!savedCategoryFilters.isEmpty()) {
            selectedFilters = new ArrayList<>(Arrays.asList(savedCategoryFilters.split(",")));
        } else {
            selectedFilters = getDefaultCategoryFilters(); // Default filters
        }
    
        // Load filetype filters
        if (!savedFileTypeFilters.isEmpty()) {
            selectedFileTypes = new ArrayList<>(Arrays.asList(savedFileTypeFilters.split(",")));
            removeUnsupportedFileTypes(); // Remove unsupported file types
        } else {
            selectedFileTypes = new ArrayList<>(); // Default to empty if not set
        }
    
        updateFilterCheckBoxes(categoryMenu, filetypeMenu);
    }
        
    private static List<String> getDefaultCategoryFilters() {
        List<String> defaultFilters = new ArrayList<>();
        defaultFilters.add("l"); // Libgen
        defaultFilters.add("c"); // Comics
        defaultFilters.add("f"); // Fiction
        defaultFilters.add("a"); // Scientific Articles
        defaultFilters.add("m"); // Magazines
        defaultFilters.add("r"); // Fiction RUS
        defaultFilters.add("s"); // Standards
        return defaultFilters;
    }    

    private static void loadMirrorUrlFromPreferences() {
        Preferences prefs = Preferences.userRoot().node("org/serverboi/infinitylibrary");
        currentMirrorUrl = prefs.get("currentMirrorUrl", "https://libgen.li/");
    }

    private static void showMirrorSelectionDialog() {
        String[] mirrors = { "https://libgen.li/", "https://libgen.gs/", "https://libgen.vg/", "https://libgen.pm/" };
        String selectedMirror = (String) JOptionPane.showInputDialog(frame,
                "Choose a mirror:",
                "Set Upstream Mirror",
                JOptionPane.QUESTION_MESSAGE,
                null,
                mirrors,
                currentMirrorUrl);
        if (selectedMirror != null && !selectedMirror.equals(currentMirrorUrl)) {
            changeMirrorUrl(selectedMirror);
            // Optionally, refresh the current view or state to reflect the new mirror
        }
    }

    // Change Mirror URL
    private static void changeMirrorUrl(String newMirrorUrl) {
        currentMirrorUrl = newMirrorUrl;
        Preferences prefs = Preferences.userRoot().node("org/serverboi/infinitylibrary");
        prefs.put("currentMirrorUrl", newMirrorUrl);
        try {
            prefs.flush(); // Save the preferences
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    // Method to create the FileType dropdown
    private static void createFileTypeDropdown(JMenu filetypeMenu) {
        String[] fileTypes = { "PDF", "DJVU", "EPUB", "MOBI", "AZW", "FB2", "RTF", "DOC", "DOCX", "ZIP",
                "RAR", "CBZ", "CBR" };


        for (String fileType : fileTypes) {
            JCheckBoxMenuItem fileTypeItem = new JCheckBoxMenuItem(fileType);
            fileTypeItem.addActionListener(e -> handleFileTypeSelection(fileTypeItem, fileType.toLowerCase()));

            // Prevent the menu from closing when an item is selected
            fileTypeItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);

            fileTypeItem.setActionCommand(fileType.toLowerCase());

            filetypeMenu.add(fileTypeItem);
        }
    }

    // Method to handle file type selection
    private static void handleFileTypeSelection(JCheckBoxMenuItem fileTypeItem, String fileType) {
        if (fileTypeItem.isSelected()) {
            selectedFileTypes.add(fileType);
        } else {
            selectedFileTypes.remove(fileType);
        }
        saveFiltersToPreferences(); // Save after each selection change
    }

    private static void updateFilterCheckBoxes(JMenu categoryMenu, JMenu filetypeMenu) {
        // Update category checkboxes
        for (int i = 0; i < categoryMenu.getItemCount(); i++) {
            JMenuItem item = categoryMenu.getItem(i);
            if (item instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) item;
                String filterValue = checkBox.getActionCommand(); // Ensure you set the ActionCommand for each checkbox
                checkBox.setSelected(selectedFilters.contains(filterValue));
            }
        }

        // Update filetype checkboxes
        for (int i = 0; i < filetypeMenu.getItemCount(); i++) {
            JMenuItem item = filetypeMenu.getItem(i);
            if (item instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem) item;
                String fileType = checkBox.getActionCommand(); // Ensure you set the ActionCommand for each checkbox
                checkBox.setSelected(selectedFileTypes.contains(fileType));
            }
        }
    }

    private static void navigatePage(int delta) {
        currentPage += delta;

        performSearch(); // Call performSearch to load the new page
    }

    private static void setLanguageCode() {
        // Create a text field for input
        JTextField inputField = new JTextField(getLanguageCodeFromPreferences(), 10);

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
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
                new Object[] {}, null);

        // Create a JDialog and set its button behavior
        final JDialog dialog = new JDialog(frame, "Set Language Code", true);
        dialog.setContentPane(optionPane);

        okButton.addActionListener(e -> {
            dialog.dispose();
            String newLanguageCode = inputField.getText().trim();
            if (newLanguageCode.matches("[a-zA-Z]{3}")) {
                languageCode = newLanguageCode.toLowerCase();
                prefs.put("languageCode", languageCode); // Save to preferences
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        optionPane.setOptions(new Object[] { okButton, cancelButton });

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static String getLanguageCodeFromPreferences() {
        Preferences prefs = Preferences.userRoot().node("org/serverboi/infinitylibrary");
        return prefs.get("languageCode", "eng"); // "eng" is the default value
    }

    private static void addFilterCheckBox(JMenu filterMenu, String label, String filterValue) {
        JCheckBoxMenuItem filterItem = new JCheckBoxMenuItem(label, true); // Set true to have it selected by default
        filterItem.setActionCommand(filterValue);
        filterItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", Boolean.TRUE);
        filterItem.addActionListener(e -> handleFilterSelection(filterItem, filterValue));

        filterMenu.add(filterItem);
    }

    private static void handleFilterSelection(JCheckBoxMenuItem filterItem, String filterValue) {
        if (filterItem.isSelected()) {
            selectedFilters.add(filterValue);
        } else {
            selectedFilters.remove(filterValue);
        }
        saveFiltersToPreferences(); // Save after each selection change
    }

    private static void saveFiltersToPreferences() {
        String joinedCategoryFilters = String.join(",", selectedFilters);
        String joinedFileTypeFilters = String.join(",", selectedFileTypes);

        System.out.println("Saving Category Filters: " + joinedCategoryFilters);
        System.out.println("Saving File Type Filters: " + joinedFileTypeFilters);

        prefs.put("selectedCategoryFilters", joinedCategoryFilters);
        prefs.put("selectedFileTypeFilters", joinedFileTypeFilters);

        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private static void performSearch() {
        String userInput = searchField.getText().trim();
        if (!userInput.isEmpty() && !isSearchInProgress) {
            showLoadingStatusLabel();
            isSearchInProgress = true;
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            updateButtonStates();

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
                        SwingUtilities.invokeLater(() -> {
                            try {
                                List<ImageDetails> imageDetailsList = get();

                                // Efficiently clear the panel on the Event Dispatch Thread
                                imagePanel.removeAll();
                                imagePanel.revalidate();
                                imagePanel.repaint();

                                if (imageDetailsList.isEmpty()) {
                                    handleNoResultsFound();
                                } else {
                                    updateSearchResults(imageDetailsList);
                                    nextButton.setEnabled(false);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                hideLoadingStatusLabel();
                                isSearchInProgress = false;
                                nextButton.setEnabled(true);
                                updateButtonStates();
                            }
                        });
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

    private static void handleNoResultsFound() {
        searchButton.setEnabled(true);
        pageCountLabel.setVisible(false);
        JOptionPane.showMessageDialog(frame, "No results found", "Search Results", JOptionPane.INFORMATION_MESSAGE);
        nextButton.setEnabled(false);
        previousButton.setEnabled(searchField.getText().trim().isEmpty() && currentPage > 1);
    }

    private static void updateSearchResults(List<ImageDetails> imageDetailsList) {
        searchButton.setEnabled(true);
        pageCountLabel.setText("Page " + currentPage);
        pageCountLabel.setVisible(true);
        previousButton.setEnabled(!searchField.getText().trim().isEmpty() && currentPage > 1);
        nextButton.setEnabled(true);

        // Populate image panel with results
        populateImagePanel(imageDetailsList);
    }

    private static ImageIcon scaleImageIcon(URL imageUrl, int maxWidth, int maxHeight) throws IOException {
        // Load the original image
        BufferedImage originalImage = ImageIO.read(imageUrl);

        // Calculate the scaling factors to fit within maxWidth and maxHeight while
        // maintaining the aspect ratio
        double widthScaleFactor = (double) maxWidth / originalImage.getWidth();
        double heightScaleFactor = (double) maxHeight / originalImage.getHeight();
        double scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);

        // Calculate the new dimensions
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);

        // Create a new buffered image with the new dimensions
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Draw the original image scaled to the new size
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return new ImageIcon(scaledImage);
    }

    private static void populateImagePanel(List<ImageDetails> imageDetailsList) {
        // Calculate the range of results to display for the current page
        int startIndex = (currentPage - 1) * RESULTS_PER_PAGE;
        int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, imageDetailsList.size());

        for (int i = startIndex; i < endIndex; i++) {
            ImageDetails details = imageDetailsList.get(i);

            SwingWorker<ImageIcon, Void> imageLoader = new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    // Load and scale the image in the background
                    URL imageUrl = new URL(details.getImageUrl());
                    return scaleImageIcon(imageUrl, 229, 327);
                }

                @Override
                protected void done() {
                    try {
                        // Update the UI with the scaled image
                        ImageIcon icon = get();
                        JLabel imageLabel = new JLabel(icon);

                        // Set cursor to hand cursor when mouse enters the label
                        imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                            public void mouseEntered(java.awt.event.MouseEvent evt) {
                                imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            }

                            public void mouseExited(java.awt.event.MouseEvent evt) {
                                imageLabel.setCursor(Cursor.getDefaultCursor());
                            }
                        });

                        imageLabel.addMouseListener(new ImageClickListener(details));
                        imagePanel.add(imageLabel);
                        imagePanel.revalidate();
                        imagePanel.repaint();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        // Handle exceptions (e.g., image loading failed)
                    }
                }
            };
            imageLoader.execute();
        }
    }

    private static void updateButtonStates() {
        boolean isSearchFieldEmpty = searchField.getText().trim().isEmpty();
        searchButton.setEnabled(!isSearchFieldEmpty && !isSearchInProgress); // Disable search button if the field is
                                                                             // empty or a search is in progress
    }

    private static void showLoadingStatusLabel() {
        loadingStatusLabel.setText("Loading...");
    }

    private static void hideLoadingStatusLabel() {
        loadingStatusLabel.setText("");
    }

    private static String constructLibGenUrl(String encodedQuery, int page) {
        StringBuilder urlBuilder = new StringBuilder(200);

        urlBuilder.append(currentMirrorUrl).append("index.php?req=")
                .append(encodedQuery)
                .append("+lang%3A")
                .append(languageCode);

        // Append file type filters
        if (!selectedFileTypes.isEmpty()) {
            for (String fileType : selectedFileTypes) {
                urlBuilder.append("+ext%3A").append(fileType);
            }
        }

        // Append other URL parameters
        urlBuilder.append(
                "&columns[]=t&columns[]=a&columns[]=s&columns[]=y&columns[]=p&columns[]=i&objects[]=f&objects[]=e&objects[]=s&objects[]=a&objects[]=p&objects[]=w");

        // Append selected topic filters
        for (String filter : selectedFilters) {
            urlBuilder.append("&topics[]=").append(filter);
        }

        urlBuilder.append("&res=25&covers=on&gmode=on&filesuns=all");

        System.out.println(urlBuilder);

        return urlBuilder.toString();
    }

    private static List<ImageDetails> scrapeLibGenImages(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements rows = document.select("table > tbody > tr");

            return rows.stream().map(row -> {
                Elements imgElement = row.select("td a img[src]");
                if (imgElement.isEmpty())
                    return null;

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
                        mirrorButton.setEnabled(false); // Disable the button when clicked
                        handleLibgenMirrorButtonClick(finalMirror, mirrorButton);
                    } else {
                        openLinkInBrowser(finalMirror);
                    }
                });

        
                buttonsPanel.add(mirrorButton);
            }
            JButton extraMirrorButton = new JButton("Audio book");
            extraMirrorButton.addActionListener(event -> {
                String userInput = searchField.getText().trim(); // Assuming searchField is your input text field
                Torrent.showResultsDialog(imageDetails.getTitle(), userInput);
            });
    
            buttonsPanel.add(extraMirrorButton);
            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BorderLayout());
            detailsPanel.add(new JTextArea(detailsMessage), BorderLayout.CENTER);
            detailsPanel.add(buttonsPanel, BorderLayout.SOUTH);

            // Create a JOptionPane
            JOptionPane pane = new JOptionPane(detailsPanel, JOptionPane.PLAIN_MESSAGE);
            pane.setOptions(new Object[] {}); // Remove default buttons

            // Create a JDialog from JOptionPane
            JDialog dialog = pane.createDialog("Book Details");

            // Load your custom icon
            ImageIcon icon = new ImageIcon(getClass().getResource("icon.png"));
            dialog.setIconImage(icon.getImage());

            // Display the dialog
            dialog.setVisible(true);
        }

        private String getBaseURL(String mirrorLink) {
            int slashIndex = mirrorLink.indexOf("/", 8);
            if (slashIndex != -1) {
                return mirrorLink.substring(0, slashIndex);
            } else {
                return mirrorLink;
            }
        }

        private void handleLibgenMirrorButtonClick(String finalMirror, JButton mirrorButton) {
            System.out.println("Mirror Link Clicked: " + finalMirror);
        
            if (selectDownloadDirectory()) {
                Thread downloadThread = new Thread(() -> {
                    Downloader.setDownloadDirectory(downloadDirectory);
        
                    if (downloadDirectory != null) {
                        // Since both conditions call the same method, the conditional check might not be necessary
                        System.out.println("Downloading from mirror: " + finalMirror);
                        Downloader.downloadFromLibgenMirror(finalMirror, ongoingDownloads, mirrorButton); // Pass the JButton
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

        private boolean selectDownloadDirectory() {
            if (downloadDirectory == null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Download Directory");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Create a custom dialog
                JDialog dialog = new JDialog();
                dialog.setTitle("Select Download Directory");
                try {
                    // Set custom icon for the dialog
                    dialog.setIconImage(ImageIO.read(
                            Thread.currentThread().getContextClassLoader().getResourceAsStream("icon.png")));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                dialog.add(fileChooser);
                dialog.pack();
                dialog.setLocationRelativeTo(null); // Center the dialog on screen

                // Show the dialog and get the user's selection
                int userSelection = fileChooser.showOpenDialog(dialog);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    downloadDirectory = fileChooser.getSelectedFile().toPath();
                    System.out.println("Download location set to: " + downloadDirectory);
                    Preferences prefs = Preferences.userRoot().node("org/serverboi/infinitylibrary");
                    prefs.put("downloadDirectory", downloadDirectory.toString());
                    return true;
                } else {
                    System.out.println("Download directory selection canceled.");
                    return false;
                }
            } else {
                return true;
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

        public ImageDetails(String imageUrl, String title, String author, String publisher, String year, String lang,
                String size, List<String> mirrors) {
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
