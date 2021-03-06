package syntax;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;

public class Interpreter {
	public static String loadFile(String filename) {
		String total = "";

		try {
			InputStream bytes = new FileInputStream(filename);
			Reader chars = new InputStreamReader(bytes, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader(chars);
			String line;
			while ((line = br.readLine()) != null)
				total = total.concat(line);
			br.close();
		} catch (IOException e) {
			System.err.println("Error openning file: " + filename);
		}
		return total;
	}

	public static Node interpret(String total, boolean auto_subscript, boolean default_color) {
		total = total.replaceAll("\\\\0", "\u2205");

		total = total.replaceAll("\\<\\-\\>", "\u2194");
		total = total.replaceAll("\\-\\>", "\u2192");
		total = total.replaceAll("\\<\\-", "\u2190");

		total = total.replaceAll("\\\\\\>", "\u27e9");
		total = total.replaceAll("\\\\\\<", "\u27e8");

		total = total.replaceAll("\\<\\=\\>", "\u21d4");
		total = total.replaceAll("\\=\\>", "\u21d2");
		total = total.replaceAll("\\<\\=", "\u21d0");
		total = total.replaceAll("\\\\w", "\u3000");

		Stack<String> tokens = new Stack<String>();
		tokens.push("");
		int mode = 0;
		for (char c : total.toCharArray()) {
			if (mode == 3) {
				if (c == '`') {
					mode = 0;
				} else {
					String s = tokens.pop();
					s = s.concat(Character.toString(c));
					tokens.push(s);
				}
				continue;
			}
			if (c == '`') {
				mode = 3;
				tokens.push("");
				continue;
			}
			if (c == '\t' || c == '\n' || c == ' ') {
				mode = 0;
				if (!tokens.peek().equals(""))
					tokens.push("");
				continue;
			}
			if (c != '[' && c != ']') {
				if (mode == 1) {
					String s = tokens.pop();
					s = s.concat(Character.toString(c));
					tokens.push(s);
				} else {
					tokens.push(Character.toString(c));
				}
				mode = 1;
			} else {
				if (tokens.peek().equals(""))
					tokens.pop();
				tokens.push(Character.toString(c));
				mode = 2;
			}
		}

		Stack<Node> stack = new Stack<Node>();

		HashMap<String, Integer> instances = new HashMap<String, Integer>();

		for (String token : tokens) {
			if (token.equals("]")) {
				ArrayList<Node> nl = new ArrayList<Node>();
				while (stack.peek().value.length() < 2 || !stack.peek().value.subSequence(0, 2).equals("$["))
					nl.add(stack.pop());
				Collections.reverse(nl);
				Node f = stack.pop();
				f.value = f.value.substring(2);

				while (f.value.startsWith(" "))
					f.value = f.value.substring(1);
				while (f.value.endsWith(" "))
					f.value = f.value.substring(0, f.value.length() - 1);

				if (f.value.split("\\^").length > 1) {
					String arr[] = f.value.split("\\^");
					f.value = arr[0];
					f.raises = new int[arr.length - 1];
					f.raisesSUB = new int[arr.length - 1];
					for (int i = 1; i < arr.length; i++) {
						String arr2[] = arr[i].split("\\,");
						f.raises[i - 1] = Integer.valueOf(arr2[0]);
						if (arr2.length > 1)
							f.raisesSUB[i - 1] = Integer.valueOf(arr2[1]);
						else
							f.raisesSUB[i - 1] = 0;
					}
				}

				if (f.value.startsWith("|")) {
					f.bracket = BRACKET.SQUARE_BRACKET;
					f.value = f.value.substring(1);
				} else if (f.value.startsWith("(")) {
					f.bracket = BRACKET.PARENTHESIS;
					f.value = f.value.substring(1);
				}

				if (!f.metadata.isEmpty()) {
					while (f.metadata.startsWith(" "))
						f.metadata = f.metadata.substring(1);
					while (f.metadata.endsWith(" "))
						f.metadata = f.metadata.substring(0, f.metadata.length() - 1);

					if (f.metadata.endsWith("^")) {
						f.mode = MODE.TRIANGLE_;
						f.metadata = f.metadata.substring(0, f.metadata.length() - 1);
					} else if (f.metadata.endsWith("|")) {
						f.mode = MODE.BAR_;
						f.metadata = f.metadata.substring(0, f.metadata.length() - 1);
					}

					if (default_color) {
						f.color = Color.BLUE;
						f.content_color = Color.getHSBColor(0.33f, 1.0f, 0.5f);
						f.connector_color = Color.BLACK;
						f.move_color = Color.RED;
					}

					if (f.metadata.startsWith("{")) {
						f.metadata = f.metadata.substring(1);
						if (!f.metadata.endsWith("}"))
							System.err.println("Unclosed options!\n" + f.metadata);
						else
							f.metadata = f.metadata.substring(0, f.metadata.length() - 1);
						String[] options = f.metadata.split("\\;(?:(?<=[\"]\\;)|(?=[\"]))");
						HashMap<String, String> values = new HashMap<String, String>();
						for (String op : options) {
							String[] temp = op.split("\\:(?:(?<=[\"]\\:)|(?=[\"]))");
							if (temp.length == 2) {
								String key = temp[0];
								String value = temp[1];
								while (key.startsWith(" "))
									key = key.substring(1);
								while (value.startsWith(" "))
									value = value.substring(1);
								while (key.endsWith(" "))
									key = key.substring(0, key.length() - 1);
								while (value.endsWith(" "))
									value = value.substring(0, value.length() - 1);
								values.put(key, value.substring(1, value.length() - 1));
							}
						}

						if (values.containsKey("content"))
							f.metadata = values.get("content");
						if (values.containsKey("color")) {
							String[] c_vals = values.get("color").split("\\,");
							int r = Integer.valueOf(c_vals[0]);
							int g = Integer.valueOf(c_vals[1]);
							int b = Integer.valueOf(c_vals[2]);
							float[] hsb = Color.RGBtoHSB(r, g, b, null);
							f.color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
						}
						if (values.containsKey("line-color")) {
							String[] c_vals = values.get("line-color").split("\\,");
							int r = Integer.valueOf(c_vals[0]);
							int g = Integer.valueOf(c_vals[1]);
							int b = Integer.valueOf(c_vals[2]);
							float[] hsb = Color.RGBtoHSB(r, g, b, null);
							f.connector_color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
						}
						if (values.containsKey("content-color")) {
							String[] c_vals = values.get("content-color").split("\\,");
							int r = Integer.valueOf(c_vals[0]);
							int g = Integer.valueOf(c_vals[1]);
							int b = Integer.valueOf(c_vals[2]);
							float[] hsb = Color.RGBtoHSB(r, g, b, null);
							f.content_color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
						}
						if (values.containsKey("move-color")) {
							String[] c_vals = values.get("move-color").split("\\,");
							int r = Integer.valueOf(c_vals[0]);
							int g = Integer.valueOf(c_vals[1]);
							int b = Integer.valueOf(c_vals[2]);
							float[] hsb = Color.RGBtoHSB(r, g, b, null);
							f.move_color = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
						}
					}
				}

				if (f.value.startsWith("@")) {
					f.bracket = BRACKET.SQUARE_BRACKET;
					f.value = f.value.substring(1);
				}

				if (!instances.containsKey(f.value)) {
					instances.put(f.value, 0);
				}
				instances.put(f.value, instances.get(f.value) + 1);
				if (auto_subscript)
					f.value = f.value + "_" + Integer.toString(instances.get(f.value)) + "_";
				f.subNodes = nl;
				for (Node n : nl)
					n.parent = f;
				stack.push(f);
			} else if (token.equals("[")) {
				stack.push(new Node("$["));
			} else if (!token.isEmpty()) {
				if (stack.peek().value.length() > 2 && stack.peek().value.subSequence(0, 2).equals("$["))
					stack.peek().metadata = stack.peek().metadata.concat(" ").concat(token);
				else
					stack.peek().value = stack.peek().value.concat(" ").concat(token);
			}
		}

		return stack.pop();
	}
}