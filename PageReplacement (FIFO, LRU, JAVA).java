/*
 * PT 101 - Final Project - Topic 3: Page Replacement (FIFO, LRU, Optimal)
 * ALL-IN-ONE single file: algorithms + Swing GUI + user guide + test cases + console fallback.
 *
 * Compile : javac Main.java
 * Run GUI : java Main            (opens the window)
 * Console : java Main console    (menu in the terminal; used automatically
 *                                 when no display is available, e.g. online compilers)
 * Tests   : java Main test
 *
 * References: Silberschatz et al., "Operating System Concepts" (page-replacement examples).
 * (Add your group's other references / AI-assistance disclosure here.)
 */
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "";
        if (mode.equals("test")) {
            System.out.println(Tests.runAll());
        } else if (mode.equals("console") || GraphicsEnvironment.isHeadless()) {
            ConsoleApp.run();
        } else {
            SwingUtilities.invokeLater(() -> new GuiApp().setVisible(true));
        }
    }
}

/* ====================================================================
 *  RESULT: everything recorded about one simulation run
 * ==================================================================== */
class Result {
    String algo;        // FIFO / LRU / OPT
    String name;        // display name
    int frameCount;
    int[] refs;
    int[][] frames;     // frames[step][slot], -1 = empty
    boolean[] hit;
    int[] replaced;     // page evicted at that step, -1 = none
    String[] notes;
    int hits, faults;

    int total() { return refs.length; }
    double hitPct() { return hits * 100.0 / total(); }
    double faultPct() { return faults * 100.0 / total(); }
    int[] finalFrames() { return frames[frames.length - 1]; }
}

/* ====================================================================
 *  ENGINE: the three algorithms (no input/output here)
 * ==================================================================== */
class Engine {
    static final String[] ALGOS = {"FIFO", "LRU", "OPT"};

    static String fullName(String a) {
        switch (a) {
            case "FIFO": return "First-In First-Out (FIFO)";
            case "LRU":  return "Least Recently Used (LRU)";
            default:     return "Optimal (OPT)";
        }
    }

    /** "7 0 1,2" -> {7,0,1,2}. Throws IllegalArgumentException on bad input. */
    static int[] parseRefs(String text) {
        String t = text.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("Reference string is empty.");
        String[] parts = t.split("[,\\s]+");
        List<Integer> out = new ArrayList<Integer>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            int v;
            try { v = Integer.parseInt(p); }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + p + "' is not a valid page number (whole numbers only).");
            }
            if (v < 0) throw new IllegalArgumentException("Page number " + v + " is negative.");
            out.add(v);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Reference string is empty.");
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        return arr;
    }

    static int parseFrames(String text) {
        int n;
        try { n = Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number of frames must be a whole number.");
        }
        if (n < 1 || n > 20) throw new IllegalArgumentException("Number of frames must be from 1 to 20.");
        return n;
    }

    static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return -1;
    }

    /** Choose which frame slot to evict (all frames are full). */
    static int victim(String algo, int[] fr, int now, int[] refs,
                      HashMap<Integer, Integer> loadTime, HashMap<Integer, Integer> lastUsed) {
        int best = 0;
        if (algo.equals("FIFO")) {            // oldest loaded
            int min = Integer.MAX_VALUE;
            for (int s = 0; s < fr.length; s++) {
                int t = loadTime.get(fr[s]);
                if (t < min) { min = t; best = s; }
            }
        } else if (algo.equals("LRU")) {      // least recently used
            int min = Integer.MAX_VALUE;
            for (int s = 0; s < fr.length; s++) {
                int t = lastUsed.get(fr[s]);
                if (t < min) { min = t; best = s; }
            }
        } else {                              // OPT: farthest next use; never used again = infinity
            int far = -1;
            for (int s = 0; s < fr.length; s++) {
                int next = Integer.MAX_VALUE;
                for (int j = now + 1; j < refs.length; j++) {
                    if (refs[j] == fr[s]) { next = j; break; }
                }
                if (next > far) { far = next; best = s; }   // ties -> lowest frame
            }
        }
        return best;
    }

    static Result simulate(String algo, int[] refs, int n) {
        int total = refs.length;
        Result r = new Result();
        r.algo = algo; r.name = fullName(algo); r.frameCount = n; r.refs = refs.clone();
        r.frames = new int[total][];
        r.hit = new boolean[total];
        r.replaced = new int[total];
        r.notes = new String[total];

        int[] fr = new int[n];
        Arrays.fill(fr, -1);
        HashMap<Integer, Integer> loadTime = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> lastUsed = new HashMap<Integer, Integer>();

        for (int i = 0; i < total; i++) {
            int page = refs[i];
            r.replaced[i] = -1;
            if (indexOf(fr, page) >= 0) {
                r.hit[i] = true;
                r.hits++;
                r.notes[i] = "Page " + page + " is already in memory.";
            } else {
                r.faults++;
                int slot = indexOf(fr, -1);
                if (slot >= 0) {
                    r.notes[i] = "Page " + page + " loaded into empty Frame " + (slot + 1) + ".";
                } else {
                    slot = victim(algo, fr, i, refs, loadTime, lastUsed);
                    r.replaced[i] = fr[slot];
                    r.notes[i] = "Frames full: replaced page " + fr[slot] + " in Frame " + (slot + 1)
                            + " with page " + page + ".";
                }
                fr[slot] = page;
                loadTime.put(page, i);
            }
            lastUsed.put(page, i);
            r.frames[i] = fr.clone();
        }
        return r;
    }

    static List<Result> runAll(int[] refs, int n) {
        List<Result> list = new ArrayList<Result>();
        for (String a : ALGOS) list.add(simulate(a, refs, n));
        return list;
    }

    static String cell(int v) { return v < 0 ? "-" : String.valueOf(v); }

    static String finalFramesText(Result r) {
        StringBuilder sb = new StringBuilder("[");
        int[] f = r.finalFrames();
        for (int i = 0; i < f.length; i++) sb.append(i > 0 ? ", " : "").append(cell(f[i]));
        return sb.append("]").toString();
    }

    static String bestText(List<Result> rs) {
        int best = Integer.MAX_VALUE;
        for (Result r : rs) best = Math.min(best, r.faults);
        StringBuilder sb = new StringBuilder();
        for (Result r : rs) if (r.faults == best) sb.append(sb.length() > 0 ? ", " : "").append(r.algo);
        return "Fewest page faults: " + sb + " (" + best + " faults).";
    }

    /** Plain-text trace used by the console version. */
    static String textTrace(Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ").append(r.name).append(" | Frames: ").append(r.frameCount).append(" ===\n");
        sb.append(String.format("%-5s%-5s", "Step", "Ref"));
        for (int k = 1; k <= r.frameCount; k++) sb.append(String.format("%-5s", "F" + k));
        sb.append("Result\n");
        for (int i = 0; i < r.total(); i++) {
            sb.append(String.format("%-5d%-5d", i + 1, r.refs[i]));
            for (int v : r.frames[i]) sb.append(String.format("%-5s", cell(v)));
            sb.append(r.hit[i] ? "HIT" : "FAULT");
            if (r.replaced[i] >= 0) sb.append("  (replaced ").append(r.replaced[i]).append(")");
            sb.append("\n");
        }
        sb.append("\nFinal frames : ").append(finalFramesText(r)).append("\n");
        sb.append(String.format("Total refs   : %d%n", r.total()));
        sb.append(String.format("Hits         : %d (%.2f%%)%n", r.hits, r.hitPct()));
        sb.append(String.format("Faults       : %d (%.2f%%)%n", r.faults, r.faultPct()));
        sb.append(String.format("Hit:Fault    : %d:%d%n", r.hits, r.faults));
        return sb.toString();
    }

    static String textComparison(List<Result> rs) {
        StringBuilder sb = new StringBuilder("\n=== COMPARISON ===\n");
        sb.append(String.format("%-28s%-7s%-8s%-9s%s%n", "Algorithm", "Hits", "Faults", "Hit %", "Fault %"));
        for (Result r : rs)
            sb.append(String.format("%-28s%-7d%-8d%-9.2f%.2f%n", r.name, r.hits, r.faults, r.hitPct(), r.faultPct()));
        sb.append("\n").append(bestText(rs)).append("\n");
        return sb.toString();
    }
}

/* ====================================================================
 *  TEST CASES (expected vs actual)
 * ==================================================================== */
class Tests {
    static final int[] TEXTBOOK = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
    static final int[] BELADY = {1, 2, 3, 4, 1, 2, 5, 1, 2, 3, 4, 5};

    static String runAll() {
        Object[][] cases = {
            {"Test 1: textbook string, 3 frames, FIFO", "FIFO", TEXTBOOK, 3, 15},
            {"Test 1: textbook string, 3 frames, LRU", "LRU", TEXTBOOK, 3, 12},
            {"Test 1: textbook string, 3 frames, OPT", "OPT", TEXTBOOK, 3, 9},
            {"Test 2: Belady string, 3 frames, FIFO", "FIFO", BELADY, 3, 9},
            {"Test 2: Belady string, 4 frames, FIFO (anomaly)", "FIFO", BELADY, 4, 10},
            {"Test 2: Belady string, 3 frames, LRU", "LRU", BELADY, 3, 10},
            {"Test 2: Belady string, 3 frames, OPT", "OPT", BELADY, 3, 7},
            {"Test 3: 5 5 5 5 with 2 frames, FIFO", "FIFO", new int[]{5, 5, 5, 5}, 2, 1},
            {"Test 3: 1 2 3 with 1 frame, LRU", "LRU", new int[]{1, 2, 3}, 1, 3},
            {"Test 3: 1 2 3 with 5 frames, OPT", "OPT", new int[]{1, 2, 3}, 5, 3},
            {"Test 4: 1 2 3 1 4, 3 frames, LRU", "LRU", new int[]{1, 2, 3, 1, 4}, 3, 4},
            {"Test 4: 1 2 3 1 4, 3 frames, FIFO", "FIFO", new int[]{1, 2, 3, 1, 4}, 3, 4},
        };
        StringBuilder sb = new StringBuilder("BUILT-IN TEST CASES (expected vs actual page faults)\n");
        sb.append("------------------------------------------------------------------------\n");
        int pass = 0;
        for (Object[] c : cases) {
            int expected = (Integer) c[4];
            int actual = Engine.simulate((String) c[1], (int[]) c[2], (Integer) c[3]).faults;
            boolean ok = expected == actual;
            if (ok) pass++;
            sb.append(String.format("%-50s exp=%-3d act=%-3d %s%n", c[0], expected, actual, ok ? "PASS" : "FAIL"));
        }
        // extra check: eviction choices
        Result lru = Engine.simulate("LRU", new int[]{1, 2, 3, 1, 4}, 3);
        boolean ok = lru.replaced[4] == 2;
        sb.append(String.format("%-50s exp=2   act=%-3d %s%n", "Test 4: LRU evicts page 2 at step 5", lru.replaced[4], ok ? "PASS" : "FAIL"));
        if (ok) pass++;
        Result fifo = Engine.simulate("FIFO", new int[]{1, 2, 3, 1, 4}, 3);
        ok = fifo.replaced[4] == 1;
        sb.append(String.format("%-50s exp=1   act=%-3d %s%n", "Test 4: FIFO evicts page 1 at step 5", fifo.replaced[4], ok ? "PASS" : "FAIL"));
        if (ok) pass++;
        int totalCases = cases.length + 2;
        sb.append("------------------------------------------------------------------------\n");
        sb.append("Passed ").append(pass).append(" of ").append(totalCases).append(" test cases.\n");
        return sb.toString();
    }
}

/* ====================================================================
 *  USER GUIDE TEXT
 * ==================================================================== */
class Guide {
    static final String TEXT =
        "USER GUIDE\n"
      + "==========\n\n"
      + "HOW TO RUN\n"
      + "  javac Main.java   then   java Main        (opens this window)\n"
      + "  java Main console                          (text menu in the terminal)\n"
      + "  java Main test                             (run the test cases only)\n\n"
      + "INPUTS\n"
      + "  Page reference string : page numbers separated by spaces or commas,\n"
      + "                          e.g.  7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1\n"
      + "                          (whole numbers, 0 or higher)\n"
      + "  Number of frames      : whole number from 1 to 20\n"
      + "  Algorithm             : FIFO, LRU, Optimal, or Compare All\n"
      + "  Click RUN SIMULATION. Invalid input shows an error message.\n\n"
      + "READING THE OUTPUT\n"
      + "  Each row of the trace is one page reference (Step 1, 2, 3 ...).\n"
      + "  Frame 1..N show what is in memory after that step ('-' = empty frame).\n"
      + "  Yellow cell  = the frame that was just loaded or replaced.\n"
      + "  Green HIT    = page was already in a frame.\n"
      + "  Red FAULT    = page was not in memory, so it had to be loaded.\n"
      + "  Details      = plain-language explanation, including which page was replaced.\n"
      + "  Below the table: final frames, total hits and faults, hit % and fault %\n"
      + "  (hit % = hits / total references x 100), and the hit:fault ratio.\n"
      + "  Compare All adds a Comparison tab listing the three algorithms side by side.\n\n"
      + "ALGORITHM RULES USED\n"
      + "  FIFO    : evict the page that has been in memory the longest.\n"
      + "            A hit does NOT refresh the page's age.\n"
      + "  LRU     : evict the page whose most recent use is the oldest.\n"
      + "            A hit DOES refresh it.\n"
      + "  Optimal : evict the page whose next use is farthest in the future.\n"
      + "            Pages never used again count as infinitely far.\n"
      + "            Ties go to the lowest-numbered frame.\n"
      + "  If a frame is empty, a fault fills the first empty frame (no eviction).\n"
      + "  The new page takes the same frame position as the page it replaces.\n";
}

/* ====================================================================
 *  GUI (Swing)
 * ==================================================================== */
class GuiApp extends JFrame {
    // ---- COLOR THEME (change these to restyle the whole app) ----
    private static final Color HEADER_A = new Color(88, 28, 135);    // deep violet
    private static final Color HEADER_B = new Color(219, 39, 119);   // magenta pink
    private static final Color ACCENT = new Color(13, 148, 136);     // teal (Run button)
    private static final Color ACCENT_2 = new Color(245, 158, 11);   // amber (Clear button)
    private static final Color BG = new Color(250, 245, 255);        // soft lavender background
    private static final Color TABLE_HEAD = new Color(109, 40, 217); // violet table header
    private static final Color GREEN = new Color(187, 247, 208);     // HIT
    private static final Color RED = new Color(254, 202, 202);       // FAULT
    private static final Color YELLOW = new Color(253, 230, 138);    // changed frame
    private static final Color ROW_ALT = new Color(245, 238, 255);   // zebra rows

    static {
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", BG);
        UIManager.put("TabbedPane.borderHightlightColor", TABLE_HEAD);
        UIManager.put("TabbedPane.focus", TABLE_HEAD);
    }

    private final JTextField refsField = new JTextField("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1");
    private final JTextField framesField = new JTextField("3");
    private final JComboBox<String> algoBox =
            new JComboBox<String>(new String[]{"FIFO", "LRU", "Optimal", "Compare All"});
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final JLabel status = new JLabel(" Ready. Enter a reference string and press RUN SIMULATION.");

    GuiApp() {
        super("Page Replacement Simulator - PT 101");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 8));
        getContentPane().setBackground(BG);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        center.add(buildInputPanel(), BorderLayout.NORTH);

        JTabbedPane main = new JTabbedPane();
        main.addTab("Simulation", resultTabs);
        main.addTab("Test Cases", buildTestPanel());
        main.addTab("User Guide", buildTextPanel(Guide.TEXT));
        center.add(main, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 6, 12));
        status.setForeground(HEADER_A);
        add(status, BorderLayout.SOUTH);

        resultTabs.addTab("Welcome", buildTextPanel(
            "Welcome!\n\nEnter a page reference string and the number of frames above,\n"
          + "choose an algorithm (or Compare All), then click RUN SIMULATION.\n\n"
          + "See the 'User Guide' tab for how to read the results and the 'Test Cases' tab\n"
          + "for expected-vs-actual evidence."));
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new GridLayout(2, 1)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, HEADER_A, getWidth(), 0, HEADER_B));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel t = new JLabel("PAGE REPLACEMENT SIMULATOR");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel s = new JLabel("PT 101 Platform Technologies   |   FIFO  -  LRU  -  OPTIMAL");
        s.setForeground(new Color(253, 230, 250));
        h.add(t);
        h.add(s);
        return h;
    }

    private JPanel buildInputPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TABLE_HEAD, 2), "Input"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.weightx = 3; c.gridx = 0; p.add(new JLabel("Page reference string:"), c);
        c.weightx = 0.5; c.gridx = 1; p.add(new JLabel("Number of frames:"), c);
        c.weightx = 1; c.gridx = 2; p.add(new JLabel("Algorithm:"), c);

        JButton run = new JButton("RUN SIMULATION");
        run.setBackground(ACCENT);
        run.setForeground(Color.WHITE);
        run.setFont(run.getFont().deriveFont(Font.BOLD));
        run.setOpaque(true);
        run.setBorderPainted(false);
        run.addActionListener(e -> runSimulation());
        refsField.addActionListener(e -> runSimulation());
        framesField.addActionListener(e -> runSimulation());

        JButton clear = new JButton("Clear");
        clear.setBackground(ACCENT_2);
        clear.setForeground(Color.WHITE);
        clear.setFont(clear.getFont().deriveFont(Font.BOLD));
        clear.setOpaque(true);
        clear.setBorderPainted(false);
        clear.addActionListener(e -> {
            refsField.setText("");
            framesField.setText("");
            resultTabs.removeAll();
            status.setText(" Cleared.");
            refsField.requestFocus();
        });

        c.gridy = 1; c.gridx = 0; c.weightx = 3; p.add(refsField, c);
        c.gridx = 1; c.weightx = 0.5; p.add(framesField, c);
        c.gridx = 2; c.weightx = 1; p.add(algoBox, c);
        JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
        btns.add(run);
        btns.add(clear);
        c.gridx = 3; c.weightx = 1.5; p.add(btns, c);
        return p;
    }

    private void runSimulation() {
        try {
            int[] refs = Engine.parseRefs(refsField.getText());
            int n = Engine.parseFrames(framesField.getText());
            String choice = (String) algoBox.getSelectedItem();

            List<Result> results = new ArrayList<Result>();
            if (choice.equals("Compare All")) results = Engine.runAll(refs, n);
            else results.add(Engine.simulate(choice.equals("Optimal") ? "OPT" : choice, refs, n));

            resultTabs.removeAll();
            for (Result r : results) resultTabs.addTab(r.algo, buildResultPanel(r));
            if (results.size() > 1) resultTabs.addTab("Comparison", buildComparisonPanel(results));
            resultTabs.setSelectedIndex(results.size() > 1 ? results.size() : 0);
            status.setText(" Simulation completed: " + refs.length + " references, " + n + " frames.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
            status.setText(" Invalid input: " + ex.getMessage());
        }
    }

    private JPanel buildResultPanel(final Result r) {
        String[] cols = new String[r.frameCount + 4];
        cols[0] = "Step";
        cols[1] = "Page";
        for (int i = 0; i < r.frameCount; i++) cols[i + 2] = "Frame " + (i + 1);
        cols[r.frameCount + 2] = "Result";
        cols[r.frameCount + 3] = "Details";

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (int i = 0; i < r.total(); i++) {
            Object[] row = new Object[cols.length];
            row[0] = i + 1;
            row[1] = r.refs[i];
            for (int k = 0; k < r.frameCount; k++) row[k + 2] = Engine.cell(r.frames[i][k]);
            row[r.frameCount + 2] = r.hit[i] ? "HIT" : "FAULT";
            row[r.frameCount + 3] = r.notes[i];
            model.addRow(row);
        }

        JTable table = new JTable(model);
        styleHeader(table);
        table.setRowHeight(26);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        for (int i = 0; i < cols.length; i++) {
            int w = i < 2 ? 55 : (i == cols.length - 1 ? 440 : 75);
            table.getColumnModel().getColumn(i).setPreferredWidth(w);
        }

        final int resultCol = r.frameCount + 2;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(col == cols.length - 1 ? LEFT : CENTER);
                comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                comp.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                comp.setForeground(new Color(30, 20, 50));
                if (col == resultCol) {
                    comp.setBackground(r.hit[row] ? GREEN : RED);
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                } else if (col >= 2 && col < resultCol && !r.hit[row]) {
                    int slot = col - 2;
                    int prev = row == 0 ? -2 : r.frames[row - 1][slot];
                    if (r.frames[row][slot] != prev) {
                        comp.setBackground(YELLOW);
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                    }
                }
                if (sel) comp.setBackground(comp.getBackground().darker());
                return comp;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(TABLE_HEAD), r.name + " - frame-by-frame trace"));

        JLabel summary = new JLabel("<html><b>Final frames:</b> " + Engine.finalFramesText(r)
                + " &nbsp;&nbsp; <b>Total refs:</b> " + r.total()
                + " &nbsp;&nbsp; <b>Hits:</b> " + r.hits + String.format(" (%.2f%%)", r.hitPct())
                + " &nbsp;&nbsp; <b>Faults:</b> " + r.faults + String.format(" (%.2f%%)", r.faultPct())
                + " &nbsp;&nbsp; <b>Hit:Fault</b> " + r.hits + ":" + r.faults + "</html>");
        summary.setFont(summary.getFont().deriveFont(14f));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        summary.setForeground(HEADER_A);

        JPanel p = new JPanel(new BorderLayout());
        p.add(sp, BorderLayout.CENTER);
        p.add(summary, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildComparisonPanel(List<Result> rs) {
        String[] cols = {"Algorithm", "Hits", "Faults", "Hit %", "Fault %"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (Result r : rs)
            m.addRow(new Object[]{r.name, r.hits, r.faults, String.format("%.2f", r.hitPct()), String.format("%.2f", r.faultPct())});
        JTable t = new JTable(m);
        styleHeader(t);
        t.setRowHeight(30);
        t.setFont(t.getFont().deriveFont(14f));
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        JLabel best = new JLabel("<html><b>" + Engine.bestText(rs) + "</b><br>Optimal is the theoretical best; it needs future "
                + "knowledge, so real systems approximate it with LRU-like policies.</html>");
        best.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        best.setForeground(HEADER_A);
        JPanel p = new JPanel(new BorderLayout());
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(TABLE_HEAD), "Comparison"));
        p.add(sp, BorderLayout.CENTER);
        p.add(best, BorderLayout.SOUTH);
        return p;
    }

    /** Colored table header (violet with white bold text). */
    private void styleHeader(JTable t) {
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, row, col);
                l.setBackground(TABLE_HEAD);
                l.setForeground(Color.WHITE);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setHorizontalAlignment(CENTER);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(167, 139, 250)));
                return l;
            }
        });
        t.setGridColor(new Color(221, 214, 254));
    }

    private JPanel buildTestPanel() {
        final JTextArea area = new JTextArea(Tests.runAll());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton again = new JButton("Run Test Cases");
        again.setBackground(ACCENT);
        again.setForeground(Color.WHITE);
        again.setOpaque(true);
        again.setBorderPainted(false);
        again.addActionListener(e -> area.setText(Tests.runAll()));
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        p.add(again, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildTextPanel(String text) {
        JTextArea a = new JTextArea(text);
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setMargin(new Insets(10, 12, 10, 12));
        a.setBackground(new Color(255, 251, 255));
        a.setCaretPosition(0);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(a), BorderLayout.CENTER);
        return p;
    }
}

/* ====================================================================
 *  CONSOLE FALLBACK (works in online compilers with no display)
 * ==================================================================== */
class ConsoleApp {
    static final String MENU =
        "\n==========================================\n"
      + "   PAGE REPLACEMENT SIMULATOR  (PT 101)\n"
      + "==========================================\n"
      + "1. FIFO\n2. LRU\n3. Optimal\n4. Compare all algorithms\n"
      + "5. Run built-in test cases\n6. User guide\n7. Exit\n";

    static void run() {
        Scanner in = new Scanner(System.in);
        try {
            while (true) {
                System.out.println(MENU);
                System.out.print("Select option (1-7): ");
                String c = in.nextLine().trim();
                if (c.equals("7")) { System.out.println("Goodbye!"); return; }
                if (c.equals("5")) { System.out.println(Tests.runAll()); }
                else if (c.equals("6")) { System.out.println(Guide.TEXT); }
                else if (c.matches("[1-4]")) {
                    int[] refs; int n;
                    while (true) {
                        try {
                            System.out.print("Page reference string (e.g. 7 0 1 2 0 3): ");
                            refs = Engine.parseRefs(in.nextLine());
                            System.out.print("Number of page frames: ");
                            n = Engine.parseFrames(in.nextLine());
                            break;
                        } catch (IllegalArgumentException e) {
                            System.out.println("  Invalid input: " + e.getMessage() + " Please try again.\n");
                        }
                    }
                    if (c.equals("4")) {
                        List<Result> rs = Engine.runAll(refs, n);
                        for (Result r : rs) System.out.println(Engine.textTrace(r));
                        System.out.println(Engine.textComparison(rs));
                    } else {
                        System.out.println(Engine.textTrace(Engine.simulate(Engine.ALGOS[Integer.parseInt(c) - 1], refs, n)));
                    }
                } else { System.out.println("Please choose a number from 1 to 7."); continue; }
                System.out.print("\nPress Enter to return to the menu...");
                in.nextLine();
            }
        } catch (java.util.NoSuchElementException e) {
            System.out.println("\n(End of input)");
        }
    }
}
