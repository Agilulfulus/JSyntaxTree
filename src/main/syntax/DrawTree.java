package syntax;

import java.awt.*;
import java.awt.font.*;
import java.text.*;
import java.util.ArrayList;
import java.awt.geom.*;

public class DrawTree {
    private int fontSize;
    private int spacingX;
    private int spacingY;
    private int border;
    private boolean in_color;
    private BasicStroke stroke;
    private Font font;
    
    private int height;
    private int width;

    private Node NS;
    public DrawTree(Node NS, int fontSize, int spacingX, int spacingY, int border, String font_name, boolean in_color, float strokeWeight) {
        this.NS = NS;
        this.font = new Font(font_name, Font.PLAIN, fontSize);
        this.fontSize = fontSize;
        this.spacingX = spacingX;
        this.spacingY = spacingY;
        this.border = border;
        this.in_color = in_color;
        this.stroke = new BasicStroke(strokeWeight);

        height = NS.getDepth(this) + border * 2 + spacingY;
        width = NS.getWidth(this) + border * 2;
    }

	public void paintStatic(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setPaint(Color.BLACK);

        NS.resetPasses();

        paintNode(border, border + fontSize / 2, g2, NS);
	}
	
	public int paintNode(int _x, int _y, Graphics2D g2, Node n) {        
        int center_x = _x + n.getWidth(this) / 2;

        ArrayList<Integer> x_vals = new ArrayList<Integer>();

        if (!n.subNodes.isEmpty()) {
            int x = _x;
            for (Node sub : n.subNodes) {
                g2.setStroke(stroke);
                //g2.draw(new Line2D.Float(center_x, _y + (int)((float)font.getSize2D() / 1.25), x + sub.getWidth(this) / 2, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));
                x_vals.add(paintNode(x, _y + spacingY, g2, sub));
                x+=sub.getWidth(this);
            }
        } else if (!n.metadata.isEmpty()) {
            if (n.metadata.charAt(n.metadata.length() - 1) == '^') {
                g2.setStroke(stroke);
                if (in_color)
                    g2.setPaint(Color.BLACK);
                g2.draw(new Line2D.Float(center_x, _y + (int)((float)font.getSize2D() / 1.25), center_x - n.getWidth(this) / 2 + spacingX / 2, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));               
                g2.draw(new Line2D.Float(center_x, _y + (int)((float)font.getSize2D() / 1.25), center_x + n.getWidth(this) / 2 - spacingX / 2, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));
                g2.draw(
                    new Line2D.Float(center_x - n.getWidth(this) / 2 + spacingX / 2, _y + spacingY - (int)((float)font.getSize2D() / 1.25), center_x + n.getWidth(this) / 2 - spacingX / 2, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));
                if (in_color)
                        g2.setPaint(Color.GREEN);
                drawCenteredString(center_x, _y + spacingY, g2, n.metadata.substring(0, n.metadata.length() - 1));
            } else if (n.metadata.charAt(n.metadata.length() - 1) == '|') {
                g2.setStroke(stroke);
                if (in_color)
                    g2.setPaint(Color.BLACK);
                g2.draw(new Line2D.Float(center_x, _y + (int)((float)font.getSize2D() / 1.25), center_x, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));
                if (in_color)
                        g2.setPaint(Color.GREEN);
                drawCenteredString(center_x, _y + spacingY, g2, n.metadata.substring(0, n.metadata.length() - 1));                
            } else {
                if (in_color)
                    g2.setPaint(Color.GREEN);
                drawCenteredString(center_x, (int)(_y + font.getSize2D() * 1.5), g2, n.metadata);
            }
        }

        int avg_x = center_x;
        
        if (!x_vals.isEmpty()) {
            if (in_color)
                g2.setPaint(Color.BLACK);
            avg_x = (x_vals.get(0) + x_vals.get(x_vals.size() - 1)) / 2;
            for (int x : x_vals) {
                g2.draw(new Line2D.Float(avg_x, _y + (int)((float)font.getSize2D() / 1.25), x, _y + spacingY - (int)((float)font.getSize2D() / 1.25)));
            }
        }
        
        if (n.raises.length > 0) {
            for (int i : n.raises) {
                Node end = n;
                for (int j = 0; j < Math.abs(i); j++)
                    end = end.getNeighborLeft();
                drawMovement(n, end, g2, i > 0);
            }
        }

        if (in_color)
            g2.setPaint(Color.BLUE);
        drawCenteredString(avg_x, _y, g2, n.value);

        return avg_x;
    }
    
    public void drawMovement(Node start, Node end, Graphics2D g2, boolean inOut) {
        int shiftY = 0;
        Node max_current = end;
        int depth_max = end.getTotalY(this) + end.getDepth(this);
        Node current = start;
        while (current != end) {
            if (depth_max < current.getTotalY(this) + current.getDepth(this)) {
                depth_max = current.getTotalY(this) + current.getDepth(this);
                max_current = current;
            }
            current = current.getNeighborLeft();
        }

        shiftY = max_current.passes * fontSize / 2;
        max_current.passes++;
        depth_max += shiftY + fontSize;

        int startX = start.getTotalX(this) - fontSize / 4;
        int endX = end.getTotalX(this) + fontSize / 4;
        int startY = start.getTotalY(this) + start.getDepth(this);// + (int)((float)font.getSize2D() * 1.5);
        int endY = end.getTotalY(this) + end.getDepth(this);// + (int)((float)font.getSize2D() * 1.5);

        if (in_color)
             g2.setPaint(Color.RED);
        //g2.draw(new QuadCurve2D.Float(startX, startY, (startX + endX) / 2, depth_max + Math.max(spacingY, Math.abs(startY - endY)), endX, endY));
       g2.draw(new Line2D.Float(startX, startY, startX, depth_max));
       g2.draw(new Line2D.Float(startX, depth_max, endX, depth_max));
       g2.draw(new Line2D.Float(endX, endY, endX, depth_max));

        if (inOut) {
            int[] x_points = {
                endX,
                endX - fontSize / 6,
                endX + fontSize / 6
            };
            int[] y_points = {
                endY,
                endY + fontSize / 4,
                endY + fontSize / 4
            };

            g2.fillPolygon(x_points, y_points, 3);
        } else {
            int[] x_points = {
                startX,
                startX - fontSize / 6,
                startX + fontSize / 6
            };
            int[] y_points = {
                startY,
                startY + fontSize / 4,
                startY + fontSize / 4
            };

            g2.fillPolygon(x_points, y_points, 3);
        }
    }
	
	public void drawCenteredString(int _x, int _y, Graphics2D g2, String text) {
        String[] arr = text.split("\\\\n");
        if (arr.length > 1) {
            for (int i = 0; i < arr.length; i++){
                drawCenteredString(_x, _y + i * (int)(font.getSize2D() * 1), g2, arr[i]);
            }
            return;
        }

        while (text.charAt(0) == ' ')
            text = text.substring(1);

        AttributedString trig = getTrig(text);
        
        FontMetrics metrics = g2.getFontMetrics(font);
        int x = _x - (int)(JSyntaxTree.GetWidthOfAttributedString(trig) / 2);
        int y = _y - ((metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(trig.getIterator(), x, y);
    }

    public AttributedString getTrig(String text) {
        String s = text.replaceAll("\\_", "");
        s = s.replaceAll("\\*", "");
        s = s.replaceAll("\\%", "");
        s = s.replaceAll("\\$", "");
        s = s.replaceAll("\\@", "");
        s = s.replaceAll("\\#", "");
        AttributedString trig = new AttributedString(s);
        trig.addAttribute(TextAttribute.FAMILY, font.getFamily());
        trig.addAttribute(TextAttribute.SIZE, font.getSize());

        boolean sub = false;
        boolean bold = false;
        boolean ital = false;
        boolean smal = false;
        boolean und = false;
        boolean green = false;
        int b = 0;
        for (int i = 0; i < text.length(); i++) {

            if (text.charAt(i) == '_') {
                sub = !sub;
            } else if (text.charAt(i) == '*') {
                bold = !bold;
            } else if (text.charAt(i) == '%') {
                ital = !ital;
            } else if (text.charAt(i) == '$') {
                smal = !smal;
            } else if (text.charAt(i) == '@') {
                und = !und;
            } else if (text.charAt(i) == '#') {
                green = !green;
            } else {
                if (s.length() <= b)
                    continue;
                if (sub)
                    trig.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, b, b + 1);
                if (bold)
                    trig.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, b, b + 1);
                if (ital)
                    trig.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, b, b + 1);
                if (smal)
                    trig.addAttribute(TextAttribute.SIZE, (int)((float)fontSize * 0.70), b, b + 1);
                if (und)
                    trig.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, b, b + 1);
                if (green)
                    trig.addAttribute(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON, b, b + 1);
                b++;
            }
        }

        if (sub || bold || ital || smal || und || green)
            System.err.println("Warning: unclosed formatting\n" + text);

        return trig;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }
    
    public int getSpacingX() {
        return this.spacingX;
    }
    
    public int getSpacingY() {
        return this.spacingY;
    }
    
    public int getFontSize() {
        return this.fontSize;
    }

    public int getBorder() {
        return this.border;
    }
}