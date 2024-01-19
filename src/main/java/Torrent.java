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

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Title", "Link"}, 0);
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

            model.addRow(new Object[]{bookTitle, magnetLink});
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

        
    
        // Displaying the series title in the JLabel
        mainPanel.add(new JLabel("Results for series title: " + title + " audio"));
        mainPanel.add(searchAndCreateLinksPanel(title + " audio", true));
    
        mainPanel.add(Box.createVerticalStrut(10)); // Spacer
    
        // Displaying the user input in the JLabel
        mainPanel.add(new JLabel("Results for user input: " + userInput + " audiobook"));
        mainPanel.add(searchAndCreateLinksPanel(userInput + " audiobook", true));

        // Custom search panel
        JPanel customSearchPanel = new JPanel();
        JTextField customSearchField = new JTextField(20);
        JButton customSearchButton = new JButton("Custom Search");
        customSearchPanel.add(customSearchField);
        customSearchPanel.add(customSearchButton);
        mainPanel.add(customSearchPanel);


        customSearchButton.addActionListener(e -> {
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
        });


    
        JScrollPane scrollableMainPanel = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollableMainPanel;
    }
}
