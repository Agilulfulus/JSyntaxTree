package syntax;

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

		total = total.replaceAll("\\\\0", "\u2205");
		return total;
	}
	
	public static Node interpret(String total, boolean auto_subscript) {
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