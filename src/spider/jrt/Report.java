package spider.jrt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;



/**
 * Toolkit to generating report from java-script type template page.<p>
 * 
 * Example:
 * <pre>
 * ----- report_template_1.jrt file -----
 * 
 *  &ltp>Counting to three:&lt/p>
 *  	&lt% for (var i=1; i&lt=jrt_data.lines; i++) { %>
 *  	 &ltp>This number is &lt%= i %>.&lt/p>
 *  	&lt% } %>
 *  	&ltp>&lt%=jrt_data.name%>&lt/p>
 *  --------------------------------------
 *  </pre>
 * using report lib:
 * <pre>
 *  String report_out = ProcessJRT.executeJRT("report_template_1.jrt", "{lines:3, name:"Truong"}");
 *  print(report_out); 
 *  </pre>
 * output:
 * <pre>
 * &ltp>Counting to three:&lt/p>
 *  &ltp>This number  is 1.&lt/p>
 *  &ltp>This number  is 2.&lt/p>
 *  &ltp> Truong &lt/p>
</pre>
 *  
 * @author tw81hc
 *
 */

public class Report {
//TODO standardlone version with gui&console
	/**
	 * Open token for code pie
	 */
	public static  String INLINE_OPEN = "<%";
	

	/**
	 * Closure token for code pie
	 */
	public static  String INLINE_CLOSE = "%>";


	public static ScriptEngine engine= null;

	//NOTE: should not change for purposes
	public static final String PRINTLN_FUNC = "__outln";
	public static final String PRINT_FUNC = "__out";
	

	public static void main(String... args) throws Exception {
		outln(System.getProperty("user.dir"));
		String emb =  " <p>Counting to three:</p>\n"+
				"  	<% for (var i=1; i<=jrt_data.lines; i++) { %>\n"+
				"  	 <p>This number is <%= i %>.</p>\n"+
				"  	<% } %>\n"+
				"  	<p><%=jrt_data.name%></p>\n";
		
		String rpt = getReport(emb, "{lines:3, name:'Truong'}");
		outln("report>>\n"+rpt);
	}
	
	/**
	 * Execute an .jrt file then return report string
	 * @param path - link to report file
	 * @param json_vars - data input as json string
	 * @return report as string
	 */
	public static String executeJRT(String path, String json_vars) {
		String report = "";
		
		try{
			//1. open file
			
			BufferedReader br = new BufferedReader(new FileReader(new File(
					path)));
	
			String content = "";
			String line = null;
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			}
			content = content.substring(0, content.length()-1);
			
			br.close();
			outln(content);
			
			//3. get report
			
	         report = getReport(content, json_vars);
		} catch(Exception e){
			report = e.toString();
			e.printStackTrace();
		}
		
		return report;
	}
	
	/**
	 * Process raw <code>rpt_template</code> on default script engine with passed <code>vars</code> variables.<p>
	 * Target to generate report isolately using parameter inputs only.  
	 * @param rpt_template - template page content
	 * @param vars - object containing input data that used in code 
	 * @return report as string
	 */
	public static String getReport(String rpt_template, String json_vars){
		String report = "";
		
		try{
			//if it runs as standard-lone program, create own engine! 
			if(engine==null){
				ScriptEngineManager factory = new ScriptEngineManager(null);
				String eng_name = System.getProperty("java.version").startsWith("1.8")?
							"Nashorn" :							//^-engine & jre ver = 1.8
							"JavaScript";							//^-engine & jre ver= ?,rhino				
		        engine = factory.getEngineByName(eng_name);
			}
			//else use anonymous engine, which get from spider
			
		        engine.eval("var jrt_data="+json_vars+";");
		        report =  getReport(engine, rpt_template);
		        
		} catch(Exception e){
			
			report = e.toString();
			e.printStackTrace();
			
		} finally{
		}
        
        return report;
	
	}
	
	/**
	 * Execute js <code>code</code> on script engine <code>engine</code> immediately on passed script engine.<p>
	 * Target to generate report with input is variables existing in current engine. 
	 * @param engine - JavaScript Engine to run the code
	 * @param rpt_template - java script code for producing report
	 * @return report as string
	 */
	public static String getReport(ScriptEngine rpt_engine, String rpt_template){
		String rpt = "";
		
		try {
			String code = toCode(rpt_template);
			
			//2. add print functions
			rpt_engine.eval(
				"var __rpt = '';\n" 		+
				"function __out(s){	__rpt = __rpt + s;	}\n"		+
				"function __outln(s){s=(s==null)? '': s;	__rpt= __rpt + s +'\\n';	}\n"
			);
		
			//3. execute code
			rpt_engine.eval(code);
			
			//4. get product
			rpt = (String)rpt_engine.get("__rpt");
			
		} catch (Exception e) {
			rpt = e.toString();
			e.printStackTrace();
		}
		
		
		return rpt; 
	}
	
	/**
	 * compile jrt text base to javascript code as string
	 * @param txt_string - template as string
	 * @return
	 */
	public static String toCode(String txt_string) {

		Pattern r = Pattern.compile(
				INLINE_OPEN+
				"(.*?)"+
				INLINE_CLOSE, 
				Pattern.DOTALL);
		Matcher m = r.matcher(txt_string);
		
		// ///////// do separation codes & text pies////////////////////////////////

		//storage of code pies
		LinkedList<String> codes = new LinkedList<String>();
		
		//storage of content pies
		LinkedList<String> texts = new LinkedList<String>();

		boolean matched = false;
		
			int start = 0; //start of new text pie
			int end;		// end of new text pie
			while (m.find()) {
				matched = true;
				
				//
				String code_pie = m.group(1);
				codes.add(code_pie);
	
				//
				String text_pie;
				end = m.start(0);
				text_pie = txt_string.substring(start, end);
				start = m.end(0);
				texts.add(text_pie);
			}
	
			// the last one
			if (start != 0) {
				String s = txt_string.substring(start);
				texts.add(s);
			}
			
		if(!matched){ //if there is no inline code so just print all as text
			texts.add(txt_string);
		}

		///////// generating js code ////////////////////////////////////////

		String all_code_lines = "";

		for (int i = 0; i < texts.size(); i++) {
			
			String t = (i < texts.size())? texts.get(i) : "";
			String c = (i < codes.size())? codes.get(i) : "";

			//------text: handle " in text
			t = t.replaceAll("\"", "\\\\\"");

			//------text: mark new line sign at the end
			boolean textEndNewLine = t.endsWith("\n") | t.endsWith("\r");
			
			//------text: compiling to print functions
			String[] parts = t.split("[\n\r]");
			
			for(int j=0; j<parts.length; j++){
				if(!parts[j].isEmpty())
					all_code_lines += PRINT_FUNC + "(" + "\"" + parts[j] + "\"" + ");";
				if(j<parts.length-1){
					all_code_lines += "\n" + PRINTLN_FUNC + "(); ";
				}
			}

			//-----code: handling code line types of <%=, @, !
			if (c.startsWith("=")) {
				c = PRINT_FUNC + "(" + c.substring(1) + ");";
			} else if (c.startsWith("--")) { // comments
				c = "/*" + c + "*/";
			} else if (c.startsWith("@")) { // directive //TODO: unknown
				c = "/*" + c + "*/";
			} else if (c.startsWith("!")) { // execute
				c = c.substring(1);
			}
			
			//-----code: output
			if (textEndNewLine) {
				all_code_lines += "\n" + c;
			} else {
				all_code_lines += c;
			}

		}

		return all_code_lines;

	}

	////////////// SHORT PRINT FOR DEBUG ///////////////////////
	private static void outln(String s) {
		System.out.println("\n---------\n" + s + "\n----------\n");
	}

	private static void out(String s) {
		System.out.print(s);
	}
}
