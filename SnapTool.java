import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SnapTool extends JFrame {

    private BufferedImage fullScreenImage;
    private BufferedImage capturedCroppedImage;
    private EditableImagePanel editorPanel;

    private JButton copyButton;
    private JButton saveButton;
    private JToggleButton penBtn, arrowBtn, rectBtn, hlBtn;
    private JButton undoBtn, clearBtn;

    private Rectangle screenVirtualBounds;

    // Phase 3: History & Settings
    private final List<HistoryItem> historyList = new ArrayList<>();
    private DefaultListModel<String> historyListModel;
    private JList<String> historyUIList;
    private File saveDirectory = new File(
            System.getProperty("user.home") + File.separator + "Pictures" + File.separator + "SnapTool");
    private TrayIcon trayIcon;

    public SnapTool() {
        setTitle("SnapTool - Phase 3 (Desktop Utility)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    setVisible(false);
                    showTrayNotification("SnapTool is running in the system tray.");
                } else {
                    System.exit(0);
                }
            }
        });

        setSize(1050, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Ensure save directory exists
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }

        // Setup System Tray
        setupSystemTray();

        // Top Main Toolbar
        JToolBar mainToolBar = new JToolBar();
        mainToolBar.setFloatable(false);

        JButton newSnipBtn = new JButton("New Snip");
        newSnipBtn.setFocusable(false);
        newSnipBtn.addActionListener(e -> startSnipping());

        copyButton = new JButton("Copy");
        copyButton.setFocusable(false);
        copyButton.setEnabled(false);
        copyButton.addActionListener(e -> copyToClipboard());

        saveButton = new JButton("Save PNG");
        saveButton.setFocusable(false);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveImage());

        JButton settingsBtn = new JButton("Settings");
        settingsBtn.setFocusable(false);
        settingsBtn.addActionListener(e -> openSettingsDialog());

        mainToolBar.add(newSnipBtn);
        mainToolBar.addSeparator();
        mainToolBar.add(copyButton);
        mainToolBar.add(saveButton);
        mainToolBar.addSeparator();
        mainToolBar.add(settingsBtn);

        // Annotation Sub-Toolbar
        JToolBar annotationBar = new JToolBar();
        annotationBar.setFloatable(false);

        ButtonGroup toolGroup = new ButtonGroup();
        penBtn = new JToggleButton("Pen", true);
        arrowBtn = new JToggleButton("Arrow");
        rectBtn = new JToggleButton("Rectangle");
        hlBtn = new JToggleButton("Highlight");

        toolGroup.add(penBtn);
        toolGroup.add(arrowBtn);
        toolGroup.add(rectBtn);
        toolGroup.add(hlBtn);

        penBtn.addActionListener(e -> setEditorTool(ToolMode.PEN));
        arrowBtn.addActionListener(e -> setEditorTool(ToolMode.ARROW));
        rectBtn.addActionListener(e -> setEditorTool(ToolMode.RECTANGLE));
        hlBtn.addActionListener(e -> setEditorTool(ToolMode.HIGHLIGHT));

        undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> {
            if (editorPanel != null)
                editorPanel.undo();
        });

        clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> {
            if (editorPanel != null)
                editorPanel.clearAnnotations();
        });

        setToolButtonsEnabled(false);

        annotationBar.add(new JLabel(" Tools: "));
        annotationBar.add(penBtn);
        annotationBar.add(arrowBtn);
        annotationBar.add(rectBtn);
        annotationBar.add(hlBtn);
        annotationBar.addSeparator();
        annotationBar.add(undoBtn);
        annotationBar.add(clearBtn);

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(mainToolBar);
        topPanel.add(annotationBar);
        add(topPanel, BorderLayout.NORTH);

        // Central Editor Area & History Sidebar Split Pane
        editorPanel = new EditableImagePanel();
        JScrollPane editorScroll = new JScrollPane(editorPanel);

        historyListModel = new DefaultListModel<>();
        historyUIList = new JList<>(historyListModel);
        historyUIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyUIList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = historyUIList.getSelectedIndex();
                if (index >= 0 && index < historyList.size()) {
                    loadHistoryItem(historyList.get(index));
                }
            }
        });

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Session History"));
        historyPanel.add(new JScrollPane(historyUIList), BorderLayout.CENTER);
        historyPanel.setPreferredSize(new Dimension(200, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, historyPanel, editorScroll);
        splitPane.setDividerLocation(210);
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported())
            return;

        PopupMenu trayMenu = new PopupMenu();
        MenuItem snipItem = new MenuItem("New Snip");
        snipItem.addActionListener(e -> {
            setVisible(true);
            setState(Frame.NORMAL);
            startSnipping();
        });

        MenuItem showItem = new MenuItem("Open SnapTool");
        showItem.addActionListener(e -> {
            setVisible(true);
            setState(Frame.NORMAL);
            toFront();
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        trayMenu.add(snipItem);
        trayMenu.add(showItem);
        trayMenu.addSeparator();
        trayMenu.add(exitItem);

        BufferedImage trayImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = trayImg.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillOval(2, 2, 12, 12);
        g2d.dispose();

        trayIcon = new TrayIcon(trayImg, "SnapTool Utility", trayMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> {
            setVisible(true);
            setState(Frame.NORMAL);
            toFront();
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void showTrayNotification(String msg) {
        if (trayIcon != null) {
            trayIcon.displayMessage("SnapTool", msg, TrayIcon.MessageType.INFO);
        }
    }

    private void setToolButtonsEnabled(boolean enabled) {
        penBtn.setEnabled(enabled);
        arrowBtn.setEnabled(enabled);
        rectBtn.setEnabled(enabled);
        hlBtn.setEnabled(enabled);
        undoBtn.setEnabled(enabled);
        clearBtn.setEnabled(enabled);
    }

    private void setEditorTool(ToolMode mode) {
        if (editorPanel != null)
            editorPanel.setCurrentTool(mode);
    }

    private void startSnipping() {
        setState(Frame.ICONIFIED);
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }

        screenVirtualBounds = getVirtualScreenBounds();

        try {
            Robot robot = new Robot();
            fullScreenImage = robot.createScreenCapture(screenVirtualBounds);
        } catch (AWTException ex) {
            JOptionPane.showMessageDialog(this, "Failed to capture screen: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            setState(Frame.NORMAL);
            return;
        }

        setState(Frame.NORMAL);
        SnippingOverlay overlay = new SnippingOverlay();
        overlay.setVisible(true);
    }

    private Rectangle getVirtualScreenBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            for (GraphicsConfiguration c : gd.getConfigurations()) {
                virtualBounds = virtualBounds.union(c.getBounds());
            }
        }
        return virtualBounds;
    }

    private void processSelection(Rectangle selection, int overlayWidth, int overlayHeight) {
        if (selection == null || selection.width <= 5 || selection.height <= 5 || fullScreenImage == null) {
            return;
        }

        int imgWidth = fullScreenImage.getWidth();
        int imgHeight = fullScreenImage.getHeight();

        double scaleX = (double) imgWidth / overlayWidth;
        double scaleY = (double) imgHeight / overlayHeight;

        int cropX = (int) Math.round(selection.x * scaleX);
        int cropY = (int) Math.round(selection.y * scaleY);
        int cropW = (int) Math.round(selection.width * scaleX);
        int cropH = (int) Math.round(selection.height * scaleY);

        cropX = Math.max(0, Math.min(cropX, imgWidth - 1));
        cropY = Math.max(0, Math.min(cropY, imgHeight - 1));
        cropW = Math.max(1, Math.min(cropW, imgWidth - cropX));
        cropH = Math.max(1, Math.min(cropH, imgHeight - cropY));

        try {
            capturedCroppedImage = fullScreenImage.getSubimage(cropX, cropY, cropW, cropH);
            editorPanel.setImage(capturedCroppedImage);
            setToolButtonsEnabled(true);
            copyButton.setEnabled(true);
            saveButton.setEnabled(true);

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            HistoryItem item = new HistoryItem("Snip " + timestamp, capturedCroppedImage);
            historyList.add(0, item);
            historyListModel.add(0, item.title);
            historyUIList.setSelectedIndex(0);

            autoSaveImage(capturedCroppedImage);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cropping image: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void autoSaveImage(BufferedImage img) {
        try {
            String timeFileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File autoFile = new File(saveDirectory, "Snip_" + timeFileFormat + ".png");
            ImageIO.write(img, "PNG", autoFile);
        } catch (IOException ignored) {
        }
    }

    private void loadHistoryItem(HistoryItem item) {
        capturedCroppedImage = item.image;
        editorPanel.setImage(capturedCroppedImage);
        setToolButtonsEnabled(true);
        copyButton.setEnabled(true);
        saveButton.setEnabled(true);
    }

    private void copyToClipboard() {
        BufferedImage finalImage = editorPanel.getFlattenedImage();
        if (finalImage == null)
            return;

        Transferable imageTransferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.imageFlavor };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor))
                    return finalImage;
                throw new UnsupportedFlavorException(flavor);
            }
        };

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imageTransferable, null);
        JOptionPane.showMessageDialog(this, "Copied annotated image to clipboard!", "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveImage() {
        BufferedImage finalImage = editorPanel.getFlattenedImage();
        if (finalImage == null)
            return;

        JFileChooser fileChooser = new JFileChooser(saveDirectory);
        fileChooser.setDialogTitle("Save Annotated Screenshot");
        fileChooser.setSelectedFile(new File("screenshot_annotated.png"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }
            try {
                ImageIO.write(finalImage, "PNG", fileToSave);
                JOptionPane.showMessageDialog(this, "Saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to save image: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "SnapTool Settings", true);
        settingsDialog.setSize(450, 200);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setLayout(new GridLayout(3, 1, 10, 10));

        JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderPanel.add(new JLabel("Save Directory: "));
        JTextField pathField = new JTextField(saveDirectory.getAbsolutePath(), 25);
        pathField.setEditable(false);
        folderPanel.add(pathField);

        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(saveDirectory);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Fixed constant name here
            if (chooser.showOpenDialog(settingsDialog) == JFileChooser.APPROVE_OPTION) {
                saveDirectory = chooser.getSelectedFile();
                pathField.setText(saveDirectory.getAbsolutePath());
            }
        });
        folderPanel.add(browseBtn);

        JPanel btnPanel = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> settingsDialog.dispose());
        btnPanel.add(closeBtn);

        settingsDialog.add(new JLabel("  Configure default storage location for auto-saved snips:", JLabel.LEFT));
        settingsDialog.add(folderPanel);
        settingsDialog.add(btnPanel);
        settingsDialog.setVisible(true);
    }

    private static class HistoryItem {
        String title;
        BufferedImage image;

        public HistoryItem(String title, BufferedImage image) {
            this.title = title;
            this.image = image;
        }
    }

    private enum ToolMode {
        PEN, ARROW, RECTANGLE, HIGHLIGHT
    }

    private interface Annotation {
        void draw(Graphics2D g2d);
    }

    private static class PenStroke implements Annotation {
        private final List<Point> points = new ArrayList<>();
        private final Color color;

        public PenStroke(Point start, Color color) {
            points.add(start);
            this.color = color;
        }

        public void addPoint(Point p) {
            points.add(p);
        }

        @Override
        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < points.size(); i++) {
                Point p1 = points.get(i - 1);
                Point p2 = points.get(i);
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private static class RectangleAnnotation implements Annotation {
        private final Rectangle rect;
        private final Color color;
        private final boolean filled;

        public RectangleAnnotation(Point p1, Point p2, Color color, boolean filled) {
            int x = Math.min(p1.x, p2.x);
            int y = Math.min(p1.y, p2.y);
            int w = Math.abs(p1.x - p2.x);
            int h = Math.abs(p1.y - p2.y);
            this.rect = new Rectangle(x, y, w, h);
            this.color = color;
            this.filled = filled;
        }

        @Override
        public void draw(Graphics2D g2d) {
            if (filled) {
                g2d.setColor(color);
                g2d.fill(rect);
            } else {
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(3));
                g2d.draw(rect);
            }
        }
    }

    private static class ArrowAnnotation implements Annotation {
        private final Point p1, p2;
        private final Color color;

        public ArrowAnnotation(Point p1, Point p2, Color color) {
            this.p1 = p1;
            this.p2 = p2;
            this.color = color;
        }

        @Override
        public void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

            double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
            int barb = 12;
            double phi = Math.toRadians(35);

            int x1 = (int) (p2.x - barb * Math.cos(angle - phi));
            int y1 = (int) (p2.y - barb * Math.sin(angle - phi));
            int x2 = (int) (p2.x - barb * Math.cos(angle + phi));
            int y2 = (int) (p2.y - barb * Math.sin(angle + phi));

            g2d.drawLine(p2.x, p2.y, x1, y1);
            g2d.drawLine(p2.x, p2.y, x2, y2);
        }
    }

    private static class EditableImagePanel extends JPanel {
        private BufferedImage image;
        private final List<Annotation> annotations = new ArrayList<>();
        private ToolMode currentTool = ToolMode.PEN;

        private Point dragStartPoint;
        private Point currentDragPoint;
        private PenStroke activePenStroke;

        public EditableImagePanel() {
            setBackground(Color.DARK_GRAY);
            setLayout(new BorderLayout());

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (image == null)
                        return;
                    dragStartPoint = e.getPoint();
                    currentDragPoint = dragStartPoint;

                    if (currentTool == ToolMode.PEN) {
                        activePenStroke = new PenStroke(dragStartPoint, Color.RED);
                        annotations.add(activePenStroke);
                    }
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (image == null)
                        return;
                    currentDragPoint = e.getPoint();

                    if (currentTool == ToolMode.PEN && activePenStroke != null) {
                        activePenStroke.addPoint(currentDragPoint);
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (image == null || dragStartPoint == null)
                        return;
                    Point endPoint = e.getPoint();

                    if (currentTool == ToolMode.RECTANGLE) {
                        annotations.add(new RectangleAnnotation(dragStartPoint, endPoint, Color.RED, false));
                    } else if (currentTool == ToolMode.HIGHLIGHT) {
                        annotations.add(
                                new RectangleAnnotation(dragStartPoint, endPoint, new Color(255, 255, 0, 100), true));
                    } else if (currentTool == ToolMode.ARROW) {
                        annotations.add(new ArrowAnnotation(dragStartPoint, endPoint, Color.BLUE));
                    }

                    dragStartPoint = null;
                    currentDragPoint = null;
                    activePenStroke = null;
                    repaint();
                }
            };

            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            annotations.clear();
            setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
            revalidate();
            repaint();
        }

        public void setCurrentTool(ToolMode tool) {
            this.currentTool = tool;
        }

        public void undo() {
            if (!annotations.isEmpty()) {
                annotations.remove(annotations.size() - 1);
                repaint();
            }
        }

        public void clearAnnotations() {
            annotations.clear();
            repaint();
        }

        public BufferedImage getFlattenedImage() {
            if (image == null)
                return null;
            BufferedImage flattened = new BufferedImage(image.getWidth(), image.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = flattened.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            for (Annotation ann : annotations) {
                ann.draw(g2d);
            }
            g2d.dispose();
            return flattened;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.drawImage(image, 0, 0, this);

                for (Annotation ann : annotations) {
                    ann.draw(g2d);
                }

                if (dragStartPoint != null && currentDragPoint != null) {
                    if (currentTool == ToolMode.RECTANGLE) {
                        new RectangleAnnotation(dragStartPoint, currentDragPoint, Color.RED, false).draw(g2d);
                    } else if (currentTool == ToolMode.HIGHLIGHT) {
                        new RectangleAnnotation(dragStartPoint, currentDragPoint, new Color(255, 255, 0, 100), true)
                                .draw(g2d);
                    } else if (currentTool == ToolMode.ARROW) {
                        new ArrowAnnotation(dragStartPoint, currentDragPoint, Color.BLUE).draw(g2d);
                    }
                }
            } else {
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Click 'New Snip' to capture your screen.", 50, 50);
            }
        }
    }

    private class SnippingOverlay extends JWindow {
        private Point startPoint;
        private Rectangle selectionRect;

        public SnippingOverlay() {
            setBounds(screenVirtualBounds);
            setAlwaysOnTop(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        dispose();
                    }
                }
            });

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    selectionRect = new Rectangle(startPoint.x, startPoint.y, 0, 0);
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (startPoint == null)
                        return;
                    int x = Math.min(startPoint.x, e.getX());
                    int y = Math.min(startPoint.y, e.getY());
                    int width = Math.abs(startPoint.x - e.getX());
                    int height = Math.abs(startPoint.y - e.getY());
                    selectionRect = new Rectangle(x, y, width, height);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (selectionRect != null) {
                        processSelection(selectionRect, getWidth(), getHeight());
                    }
                    dispose();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(fullScreenImage, 0, 0, getWidth(), getHeight(), null);
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (selectionRect != null) {
                g2d.drawImage(
                        fullScreenImage,
                        selectionRect.x, selectionRect.y,
                        selectionRect.x + selectionRect.width, selectionRect.y + selectionRect.height,
                        (int) (selectionRect.x * ((double) fullScreenImage.getWidth() / getWidth())),
                        (int) (selectionRect.y * ((double) fullScreenImage.getHeight() / getHeight())),
                        (int) ((selectionRect.x + selectionRect.width)
                                * ((double) fullScreenImage.getWidth() / getWidth())),
                        (int) ((selectionRect.y + selectionRect.height)
                                * ((double) fullScreenImage.getHeight() / getHeight())),
                        null);

                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new SnapTool().setVisible(true);
        });
    }
}
