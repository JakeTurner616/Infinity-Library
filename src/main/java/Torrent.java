import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.DefaultTableModel;
import java.net.URI;
import java.awt.event.MouseMotionAdapter;

public class Torrent {

    private static JComponent searchAndCreateLinksPanel(String searchQuery, boolean retryWithoutAudiobook) {
        Elements searchResults = performSearch(searchQuery);

        if (searchResults.isEmpty() && retryWithoutAudiobook) {
            return searchAndCreateLinksPanel(searchQuery.replace(" audiobook", ""), false);
        }

        if (searchResults.isEmpty()) {
            return new JLabel("No results found");
        }

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Title", "Link", "Downloads", "Size", "Seeders", "Leechers", "Date"}, 0);
        JTable table = new JTable(model) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 1) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        for (Element result : searchResults) {
            String bookTitle = result.select(".title a").text();
            String magnetLink = result.select(".links a.dl-magnet").attr("href");
            String downloads = result.select(".stats div:nth-child(1)").text();
            String size = result.select(".stats div:nth-child(2)").text();
            String seeders = result.select(".stats div:nth-child(3) font").text();
            String leechers = result.select(".stats div:nth-child(4) font").text();
            String date = result.select(".stats div:nth-child(5)").text();

            model.addRow(new Object[]{bookTitle, magnetLink, downloads, size, seeders, leechers, date});
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && table.columnAtPoint(e.getPoint()) == 1) {
                    try {
                        Desktop.getDesktop().browse(new URI(searchResults.get(row).select(".links a.dl-magnet").attr("href")));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    }

    private static Elements performSearch(String searchQuery) {
        try {
            String url = "https://bitsearch.to/search?q=" + searchQuery.replace(" ", "+");
            System.out.println("Bitsearch url: " + url);
            Document doc = Jsoup.connect(url).get();
            return doc.select("li.card.search-result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Elements(); // Return empty result set in case of an error
    }

    public static JComponent searchAndPrintTorrentLinks(String title, String userInput) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Displaying the series title and user input results
        mainPanel.add(new JLabel("Results for series title: " + title + " audiobook"));
        mainPanel.add(searchAndCreateLinksPanel(title + " audiobook", true));
        mainPanel.add(Box.createVerticalStrut(10)); // Spacer
        mainPanel.add(new JLabel("Results for user input: " + userInput + " audiobook"));
        mainPanel.add(searchAndCreateLinksPanel(userInput + " audiobook", true));

        // Custom search button
        JButton customSearchButton = new JButton("Custom Search");
        mainPanel.add(customSearchButton);

        customSearchButton.addActionListener(e -> openCustomSearchDialog(mainPanel));

        JScrollPane scrollableMainPanel = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollableMainPanel;
    }
    public static JMenuBar getCustomSearchMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem customSearchMenuItem = new JMenuItem("Custom Search");

        customSearchMenuItem.addActionListener(e -> openCustomSearchDialog(null));

        menu.add(customSearchMenuItem);
        menuBar.add(menu);

        return menuBar;
    }
    private static void openCustomSearchDialog(JPanel mainPanel) {
        // Popup dialog for custom search
        JTextField customSearchField = new JTextField(20);
        int result = JOptionPane.showConfirmDialog(null, customSearchField, "Enter Search Query", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String customQuery = customSearchField.getText().trim();
            if (!customQuery.isEmpty()) {
                // Clear the existing components in mainPanel
                mainPanel.removeAll();

                // Re-add components with the new custom search
                mainPanel.add(new JLabel("Results for custom search: " + customQuery));
                mainPanel.add(searchAndCreateLinksPanel(customQuery + " audiobook", true));

                mainPanel.revalidate();
                mainPanel.repaint();
            }
        }
    }

    static void showResultsDialog(String title, String userInput) {
        JDialog resultsDialog = new JDialog();
        resultsDialog.setTitle("Search Results");
        resultsDialog.setModal(true);
        resultsDialog.setLayout(new BorderLayout());

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.add(searchAndCreateLinksPanel(title + " audiobook", true));
        resultsPanel.add(searchAndCreateLinksPanel(userInput + " audiobook", true));

        JScrollPane scrollPane = new JScrollPane(resultsPanel);
        resultsDialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton customSearchButton = new JButton("Custom Search");
        JButton closeButton = new JButton("Close");

        customSearchButton.addActionListener(e -> openCustomSearchDialog(resultsPanel, resultsDialog));
        closeButton.addActionListener(e -> resultsDialog.dispose());

        buttonPanel.add(customSearchButton);
        buttonPanel.add(closeButton);
        resultsDialog.add(buttonPanel, BorderLayout.SOUTH);

        resultsDialog.setSize(400, 300);
        resultsDialog.setLocationRelativeTo(null);
        resultsDialog.setVisible(true);
    }
    public static void openAudiobookSearchDialog() {
        JTextField searchField = new JTextField(20);
        int result = JOptionPane.showConfirmDialog(null, searchField, "Enter Audiobook Search Query", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String searchQuery = searchField.getText().trim();
            if (!searchQuery.isEmpty()) {
                // Perform the search and display the results
                JComponent resultsComponent = searchAndCreateLinksPanel(searchQuery + " audiobook", true);
                showSearchResultsInNewWindow(resultsComponent, "Search Results: " + searchQuery);
            }
        }
    }

    private static void showSearchResultsInNewWindow(JComponent component, String title) {
        JDialog resultsDialog = new JDialog();
        resultsDialog.setTitle(title);
        resultsDialog.setLayout(new BorderLayout());
        resultsDialog.add(new JScrollPane(component), BorderLayout.CENTER);
        resultsDialog.setSize(400, 300);
        resultsDialog.setLocationRelativeTo(null);
        resultsDialog.setVisible(true);
    }
    static void openCustomSearchDialog(JPanel resultsPanel, JDialog parentDialog) {
        JTextField customSearchField = new JTextField(20);
        int result = JOptionPane.showConfirmDialog(parentDialog, customSearchField, "Enter Search Query", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String customQuery = customSearchField.getText().trim();
            if (!customQuery.isEmpty()) {
                resultsPanel.removeAll();
                resultsPanel.add(searchAndCreateLinksPanel(customQuery, true));
                resultsPanel.revalidate();
                resultsPanel.repaint();
                parentDialog.pack();
            }
        }
    }
}