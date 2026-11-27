package chatwave.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/** Bảng dữ liệu phẳng, hiện đại (không kẻ ô), có "viên thuốc" màu cho method / trạng thái. */
public class DataTable extends JTable {
    public static final int TEXT = 0, METHOD = 1, STATUS = 2, MUTED = 3;
    private final int[] kinds;

    public DataTable(String[] cols, int[] kinds, int[] widths) {
        super(new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        this.kinds = kinds;
        setRowHeight(40);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setOpaque(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFocusable(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JTableHeader h = getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 38));
        h.setDefaultRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, final Object v, boolean s, boolean f, int r, final int c) {
                return new JComponent() {
                    @Override protected void paintComponent(Graphics g0) {
                        Graphics2D g = Gfx.g2(g0);
                        g.setColor(Theme.surface2());
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setFont(Theme.font(java.awt.Font.BOLD, 12f));
                        g.setColor(Theme.muted());
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(String.valueOf(v).toUpperCase(), 14, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                        g.dispose();
                    }
                };
            }
        });
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            TableColumn tc = getColumnModel().getColumn(i);
            tc.setPreferredWidth(widths[i]);
            tc.setMinWidth(Math.min(widths[i], 70));
        }
        setDefaultRenderer(Object.class, new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, final Object v, final boolean sel, boolean f, final int row, final int col) {
                final int kind = DataTable.this.kinds[col];
                return new JComponent() {
                    @Override protected void paintComponent(Graphics g0) {
                        Graphics2D g = Gfx.g2(g0);
                        int w = getWidth(), h = getHeight();
                        if (sel) { g.setColor(Theme.accentSoft()); g.fillRect(0, 0, w, h); }
                        else if (row == hoverRow) { g.setColor(Theme.hover()); g.fillRect(0, 0, w, h); }
                        g.setColor(Theme.border());
                        g.fillRect(0, h - 1, w, 1);
                        String s = String.valueOf(v);
                        g.setFont(Theme.font(kind == MUTED ? java.awt.Font.PLAIN : java.awt.Font.PLAIN, 13.5f));
                        FontMetrics fm = g.getFontMetrics();
                        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                        if (kind == METHOD || kind == STATUS) {
                            Color c = Theme.muted();
                            if (kind == METHOD) {
                                c = s.equals("GET") ? Theme.accent() : s.equals("POST") ? Theme.ok() : s.equals("DELETE") ? Theme.danger() : Theme.warn();
                            } else {
                                try {
                                    int code = Integer.parseInt(s);
                                    c = code >= 500 ? Theme.danger() : code >= 400 ? Theme.warn() : Theme.ok();
                                } catch (NumberFormatException e) { /* giữ màu xám */ }
                            }
                            g.setFont(Theme.font(java.awt.Font.BOLD, 11.5f));
                            fm = g.getFontMetrics();
                            int pw = fm.stringWidth(s) + 18;
                            g.setColor(Theme.alpha(c, 36));
                            g.fill(Gfx.rr(10, h / 2f - 11, pw, 22, 11));
                            g.setColor(c);
                            g.drawString(s, 19, (h - fm.getHeight()) / 2 + fm.getAscent());
                        } else {
                            g.setColor(kind == MUTED ? Theme.muted() : Theme.text());
                            g.drawString(Gfx.ellipsize(s, fm, w - 24), 14, ty);
                        }
                        g.dispose();
                    }
                };
            }
        });
        addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) { int r = rowAtPoint(e.getPoint()); if (r != hoverRow) { hoverRow = r; repaint(); } }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) { hoverRow = -1; repaint(); }
        });
    }

    private int hoverRow = -1;

    public void setRows(java.util.List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) getModel();
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    public JScrollPane scroll() {
        JScrollPane sp = ModernScroll.wrap(this);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }
}
