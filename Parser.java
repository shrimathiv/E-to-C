import java.util.*;
import java.lang.*; 

public class Parser {
	
    private Symtab symtab = new Symtab();
   // private Entry entry_obj = new Entry();

    // the first sets.
    // note: we cheat sometimes:
    // when there is only a single token in the set,
    // we generally just compare tkrep with the first token.
    TK f_declarations[] = {TK.VAR, TK.none};
    TK f_predef[] = {TK.MODULO,TK.MAX, TK.none}; //part14
    TK f_statement[] = {TK.ID, TK.PRINT, TK.SKIP, TK.STOP, TK.IF, TK.DO, TK.FA, TK.BREAK,TK.DUMP,TK.EXCNT, TK.none};
    TK f_print[] = {TK.PRINT, TK.none};
    TK f_assignment[] = {TK.ID, TK.none};
    TK f_if[] = {TK.IF, TK.none};
    TK f_do[] = {TK.DO, TK.none};
    TK f_fa[] = {TK.FA, TK.none};
    TK f_skip[] = {TK.SKIP, TK.none};	//part11
    TK f_stop[] = {TK.STOP, TK.none}; 	//part12
    TK f_break[] = {TK.BREAK, TK.none}; //part16
    TK f_dump[] ={TK.DUMP, TK.none};	//part17, part18
    TK f_expression[] = {TK.ID, TK.NUM, TK.LPAREN, TK.none};
    TK f_excnt[] = {TK.EXCNT, TK.none};

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }


    //creating our global variables, arraylist, stack
    public static long count = 0; //used to keep track of how many times "MAX" appears
    public static long flag = 0;  //used to keep track if "break" is inside any level or not 
    public static int var = 0;
    public static int ex_count=0;
    public static long i=0;
    public static int breakloop = 0;	//loop for break[num]
    public static int breaklevel = 0;	//level of the loops 
    public static int setlineNumm=0;


    ArrayList <Integer> arrayBreak = new ArrayList<Integer>();   //tracking breakloop #'s

    Stack<Integer> st = new Stack<Integer>();    //pushing/popping respective labels

    public static int javaA[]=new int[1000];



    private Scan scanner;

    Parser(Scan scanner) {
        this.scanner = scanner;
        scan();
        program();
        if( tok.kind != TK.EOF )
            parse_error("junk after logical end of program");
        symtab.reportVariables();
    }

    // print something in the generated code
    private void gcprint(String str) {
        System.out.println(str);
    }
    // print identifier in the generated code
    // it prefixes x_ in case id conflicts with C keyword.
    private void gcprintid(String str) {
        System.out.println("x_"+str);
    }

    private void program() {
        // generate the E math functions:
        gcprint("int esquare(int x){ return x*x;}");
        gcprint("#include <math.h>");
  	gcprint("#include <string.h>");
	gcprint("int esqrt(int x){ double y; if (x < 0) return 0; y = sqrt((double)x); return (int)y;}");
	gcprint("#include <stdlib.h>");
        gcprint("#include <stdio.h>");

	//part14
        //Generated c code for Modulo operator considers different possible combinations of sign of its arguments
	gcprint("int mod(int x, int y) {int z = 0; int temp = 0; if (y == 0) {printf(\"\\nmod(a,b) with b=0\\n\"); exit(1);} if (x == 0) {z = 0;} if (x > 0 && y > 0) {z = x; while (z - y >= 0) {z = z - y;}} else if (x < 0 && y < 0) {z = x; while (z - y <= 0) {z = z-y;}} else if (x < 0 && y > 0) {temp = x; while (temp < y && temp < 0) {temp = temp + y; z = temp;}} else if (x > 0 && y < 0) {temp = x; while (temp > y && temp > 0) {temp = temp + y; z = temp;}} return z;}");  //part14
	
	gcprint("#define max(a,b) ((a>b)?a:b)"); //part15 max macro
	gcprint("int counter[100]={0};");
	gcprint("int setlineNum[100]={0};");
	gcprint("int ex_count="+ex_count+";");


	//part 21
	
	//gcprint("int setlineNum="+setlineNum+";");

	gcprint("void print();");

	gcprint("int main() {");
	
		
	block();
  
	
	 
	//gcprint("for(int i=0; i<"+ex_count+";i++){     setlineNum[i]="+javaA+"[i];    }   " );

	copyValue();
	gcprint("print();");

        gcprint("return 0; }");

	gcprint("void print(){ if(ex_count>0){ printf(\"---- Execution Counts ----\\n\"); 	 printf(\" num line    count\\n\");	}	  for(int i=0;i<"+ex_count+";i++){ 	printf(\"%4d %4d %8d\\n\",i+1, setlineNum[i],counter[i]);   }	 }");//part20
      

    }

     //copy values from JavaA to C's setlineNum array for part 21
     private void copyValue(){
		System.err.println( "ex_count: "+ ex_count );
		for(int i=0; i<ex_count;i++){
		
			gcprint("setlineNum["+i+"]="+javaA[i]+";");	
			gcprint(" printf(\"setlineNum[%d]: %d\\n\","+i+",setlineNum["+i+"]);");
		}


	}

    private void block() {
        symtab.begin_st_block();
	gcprint("{");
        
	if( first(f_declarations) ) {
            declarations();
        }
        statement_list();
        symtab.end_st_block();


	gcprint("}");
    }

    private void declarations() {
        mustbe(TK.VAR);
        while( is(TK.ID) ) {
            if( symtab.add_var_entry(tok.string,tok.lineNumber) ) {
                gcprint("int");
                gcprintid(tok.string);
                gcprint("= -12345;");
            }
            scan();
        }
        mustbe(TK.RAV);
    }

    private void statement_list(){
        while( first(f_statement) ) {
            statement();
        }
    }

    private void statement(){
        if( first(f_assignment) )
            assignment();
        else if( first(f_print) )
            print();
	//part11
        else if( first(f_skip) )
            skip();
	//part12
        else if( first(f_stop) )
            stop();
        else if( first(f_if) )
            ifproc();
        else if( first(f_do) )
            doproc();
        else if( first(f_fa) )
            fa();
	//part16
	else if( first(f_break) )
            breakproc();
	//part17
	else if( first(f_dump) )
            dump();
	else if( first(f_excnt) )
	    excnt();
        else
            parse_error("statement");
    }

    private void assignment(){
        String id = tok.string;
        int lno = tok.lineNumber; // save it too before mustbe!
        mustbe(TK.ID);
        referenced_id(id, true, lno);
        gcprintid(id);
        mustbe(TK.ASSIGN);
        gcprint("=");
        expression();
        gcprint(";");
    }

    private void print(){
        mustbe(TK.PRINT);
        gcprint("printf(\"%d\\n\", ");
        expression();
        gcprint(");");
	count = 0;
    }

    private void ifproc(){
        mustbe(TK.IF);       
	guarded_commands(TK.IF);
        mustbe(TK.FI);
    }

    //part11
    private void skip(){
        mustbe(TK.SKIP);   
    }

//part20
	private void excnt(){
		
		setlineNumm=tok.lineNumber;
		
		javaA[ex_count]=setlineNumm;
		if(is(TK.EXCNT)){

			if (ex_count >= 100)
			{
				System.err.println( "can't parse: line "+ (tok.lineNumber) + " " + "too many EXCNT (more than 100)" );
				System.exit(1);	
			}
				
			
			gcprint("counter["+ex_count+"]=counter["+ex_count+"] + 1;");
			
			ex_count++;
			scan();	
		}	
		gcprint("ex_count="+ex_count+";");
	
    }


    //part16,17
    private void breakproc(){
	int setlineNum =tok.lineNumber; //saving the tok.lineNumber for warning messages
        mustbe(TK.BREAK);
	int num=-99;

	//part17 when break [number]
	if(is(TK.NUM)){	
		int loopnum;
		String id =tok.string;
		num=Integer.parseInt(id);
		scan();
		if (num==0){ 
			System.err.println( "warning: on line " + setlineNum + " " + "break 0 statement ignored");

		}
                else if ((num == breaklevel) && (first(f_statement))){
                        System.err.println( "warning: on line " + tok.lineNumber + " " + "statement(s) follows break statement" );
			loopnum = arrayBreak.get((breaklevel)-num);
			gcprint("goto label"+loopnum+";");
                }
		else if ((num>0) && (num <= breaklevel)){
			loopnum = arrayBreak.get((breaklevel)-num);
			gcprint("goto label"+loopnum+";");
		}
		else if(num>breaklevel){
			System.err.println( "warning: on line " + setlineNum + " " + "break statement exceeding loop nesting ignored");
		}
	}

	//part 16 when break by itself

       //print warning message if "break" outside loop and statement following
	else if(flag>0 && first(f_statement)){
		System.err.println( "warning: on line " + tok.lineNumber + " " + "statement(s) follows break statement" );
		gcprint("break;");
        }

        //print warning message if "break" outside loop	
	else if (flag==0){	
		System.err.println( "warning: on line " + setlineNum + " " + "break statement outside of loop ignored" );
	}

        //else print "break" in C code
	else {
		gcprint("break;");
	}
    }

    //part12
    private void stop(){

        mustbe(TK.STOP);

	//modified to print ex_count table before stop via this print()
	if(ex_count>0){
		copyValue();
		gcprint("print();");
	}

	gcprint("exit(0);");

	if( first(f_statement) ){
		System.err.println( "warning: on line " + tok.lineNumber + " " + "statement(s) follows stop statement" );		
	}
    }

    //part18
    private void dump(){
	int num=-99;
	int setlineNumDump =tok.lineNumber;
	mustbe(TK.DUMP);
			
	//Create and Get lists from Symtab	
	ArrayList<String> SymName = new ArrayList<String>();
	ArrayList<String> tempSymName = new ArrayList<String>();
	ArrayList<Integer> SymLine = new ArrayList<Integer>();
	ArrayList<Integer> SymLevel = new ArrayList<Integer>();
	
	SymName = symtab.EntryName();
	SymLine = symtab.EntryLine();
	SymLevel = symtab.EntryLevel();
	
	//use stack for reverse order of variables
	Stack<String> StackName = new Stack<String>();
	Stack<Integer> StackLine = new Stack<Integer>();
	Stack<Integer> StackLevel = new Stack<Integer>();

	String variableName = "";
	int variableLine=0;
	int variableLevel=0;
		
	//part19 takecare of dump [number]
	if(is(TK.NUM)){	
		String id =tok.string;
		num=Integer.parseInt(id);
		scan();

		for (int i = SymName.size()-1; i >= 0; i--) //newest -> oldest
		{
			//check if level matches num for local scoping
			if (SymLevel.get(i)==num){
				StackName.push(SymName.get(i));
				StackLine.push(SymLine.get(i));
				StackLevel.push(SymLevel.get(i)); 
			}	
		}

		//top dump ++++++ line
		if (num<breaklevel){ //num less than current level
			gcprint(" printf(\"+++ dump on line "+setlineNumDump+" of level "+(num)+" begin +++\\n\"); " );
		}
		if (num==breaklevel){ //num equals to current level
			gcprint(" printf(\"+++ dump on line "+setlineNumDump+" of level "+(num)+" begin +++\\n\"); " );
		}
		else if (num > breaklevel){ //num exceeds current level
                 	System.err.println( "warning: on line " + setlineNumDump + " dump statement level (" + num + ") exceeds block nesting level (" + breaklevel + "). using " + breaklevel);
              		 gcprint(" printf(\"+++ dump on line "+setlineNumDump+" of level "+breaklevel+" begin +++\\n\"); " );
		}

		//popping stack to print out values
		while (!StackName.empty()) //popping oldest first
		{
			variableName = StackName.pop();
			variableLine= StackLine.pop();
			variableLevel= StackLevel.pop();
			var++;

			gcprint("char getString_"+var+"_"+variableName+"[]=\""+variableName+"\";");
			gcprint(" printf(\"%12d %3d %3d %s\\n\","+"x_"+variableName+","+variableLine+","+variableLevel+","+"getString_"+var+"_"+variableName+");");

		}
		//bottom dump ---------- line

		if (num > breaklevel){               		 
			for (int i = SymName.size()-1; i >= 0; i--) //newest -> oldest
			{
				//check if each var name in SymName is in tempName
				if (SymLevel.get(i)==breaklevel){
					StackName.push(SymName.get(i));
					StackLine.push(SymLine.get(i));
					StackLevel.push(SymLevel.get(i)); 
				}	
			}
			while (!StackName.empty()) //popping oldest first
			{
				variableName = StackName.pop();
				variableLine= StackLine.pop();
				variableLevel= StackLevel.pop();
				var++;
			
				gcprint("char getString_"+var+"_"+variableName+"[]=\""+variableName+"\";");
				gcprint(" printf(\"%12d %3d %3d %s\\n\","+"x_"+variableName+","+variableLine+","+variableLevel+","+"getString_"+var+"_"+variableName+");"); 

			}

			gcprint(" printf(\"--- dump on line "+setlineNumDump+ " of level "+breaklevel+" end ---\\n\");");	
		}
		else if (num<breaklevel){
			gcprint(" printf(\"--- dump on line "+setlineNumDump+ " of level "+num+" end ---\\n\");");	
		}
		else if (num==breaklevel){
			gcprint(" printf(\"--- dump on line "+setlineNumDump+ " of level "+(num)+" end ---\\n\");");
		}

	}


	//part18 without [number]
	else {
		for (int i = SymName.size()-1; i >= 0; i--) //newest -> oldest
		{
			//check if each var name in SymName is in tempName
			tempSymName.add(SymName.get(i));
			StackName.push(SymName.get(i));
			StackLine.push(SymLine.get(i));
			StackLevel.push(SymLevel.get(i)); 	
		}

		gcprint(" printf(\"+++ dump on line "+setlineNumDump+" of all levels begin +++\\n\"); " );
		while (!StackName.empty()) //popping oldest first
		{
			variableName = StackName.pop();
			variableLine= StackLine.pop();
			variableLevel= StackLevel.pop();
			var++;
			
			gcprint("char getString_"+var+"_"+variableName+"[]=\""+variableName+"\";");
			gcprint(" printf(\"%12d %3d %3d %s\\n\","+"x_"+variableName+","+variableLine+","+variableLevel+","+"getString_"+var+"_"+variableName+");");  

		}
		gcprint(" printf(\"--- dump on line "+setlineNumDump+ " of all levels end ---\\n\");");	
	}//else

    }


     //modified function for break parts 16, part17
    private void doproc(){
	//same changes to flag, breaklevel, breakloop, arrayBreak as in for loop
        mustbe(TK.DO);
	
        gcprint("while(1){");
	flag++; //increment flag when entering loop

	breaklevel++; //incrementing depth by 1 level
	breakloop++; //assigning next loop number to for loop
	arrayBreak.add(breakloop);
	st.push(breakloop);
	
        guarded_commands(TK.DO);
        gcprint("}");
        mustbe(TK.OD);
	
	breaklevel--; //decrementing depth by 1 level
        gcprint("label"+st.pop()+":;");
	arrayBreak.remove(arrayBreak.size() - 1); 
	flag--; //decrement flag when exiting loop
	
    }

    //modified function for break parts 16, part17
    private void fa(){
        mustbe(TK.FA);
	
        gcprint("for(");
	flag++; //we're in 1 more for loop now
	
	breaklevel++; //incrementing depth by 1 level
	breakloop++; //assigning next loop number to for loop
	arrayBreak.add(breakloop); //adding this loop number to ArrayList
	st.push(breakloop); //pushing it onto stack

        String id = tok.string;
        int lno = tok.lineNumber; // save it too before mustbe!
        mustbe(TK.ID);
        referenced_id(id, true, lno);
        gcprintid(id);
        mustbe(TK.ASSIGN);
        gcprint("=");
        expression();
        gcprint(";");
        mustbe(TK.TO);
        gcprintid(id);
        gcprint("<=");
        expression();
        gcprint(";");
        gcprintid(id);
        gcprint("++)");
        if( is(TK.ST) ) {
            gcprint("if( ");
            scan();
            expression();
            gcprint(")");
        }
        commands();
        mustbe(TK.AF);
	breaklevel--; //decrementing depth by 1 level
        gcprint("label"+st.pop()+":;"); //popping respective loop number we're in and printing label after end of this loop

	arrayBreak.remove(arrayBreak.size() - 1); 
        flag--; //we're in 1 less for loop now
    }

    private void guarded_commands(TK which){
        guarded_command();
        while( is(TK.BOX) ) {
            scan();
            gcprint("else");
            guarded_command();
        }
        if( is(TK.ELSE) ) {
            gcprint("else");
            scan();
            commands();
        }
        else if( which == TK.DO )
            gcprint("else break;");
    }

    private void guarded_command(){
        gcprint("if(");
        expression();
        gcprint(")");
        commands();
    }

    private void commands(){
        mustbe(TK.ARROW);
        gcprint("{");/* note: generate {} to simplify, e.g., st in fa. */
        block();
        gcprint("}");
    }

    private void expression(){
        simple();
        while( is(TK.EQ) || is(TK.LT) || is(TK.GT) ||
               is(TK.NE) || is(TK.LE) || is(TK.GE)) {
            if( is(TK.EQ) ) gcprint("==");
            else if( is(TK.NE) ) gcprint("!=");
            else gcprint(tok.string);
            scan();
            simple();
        }
    }

    private void simple(){
        term();
        while( is(TK.PLUS) || is(TK.MINUS) ) {
            gcprint(tok.string);
            scan();
            term();
        }
    }

    private void term(){
        factor();
        while(  is(TK.TIMES) || is(TK.DIVIDE) || is(TK.MODE) ) {	//part13
            gcprint(tok.string);
            scan();
            factor();
        }
    }

    //part14 subfuction
    private void predef(){	
	if(is(TK.MODULO))
	{
		gcprint("mod");
		scan();
		mustbe(TK.LPAREN);
		gcprint("(");
		expression();
		mustbe(TK.COMMA);
		gcprint(",");
		expression();
		mustbe(TK.RPAREN);
		gcprint(")");
	}
	//part15 max subfunction
	else if(is(TK.MAX))
	{ 
		gcprint("max");
		scan();
		mustbe(TK.LPAREN); 
		gcprint("(");
         	expression();
        	mustbe(TK.COMMA);
	 	gcprint(",");
         	expression();
         	mustbe(TK.RPAREN);
           	gcprint(")");	
		
		//print warning message if more than 5 Max expressions nested
		if (count > 5){
			System.err.println( "can't parse: line "+ (tok.lineNumber-1) + " " + "max expressions nested too deeply (> 5 deep)" );
			System.exit(1);	
		}
		count++;  //increment count for total number of Max								
	}
     }

     private void factor(){	
        if( is(TK.LPAREN) ) {
            gcprint("(");
            scan();
            expression();
            mustbe(TK.RPAREN);
            gcprint(")");
        }
        else if( is(TK.SQUARE) ) {
            gcprint("esquare(");
            scan();
            expression();
            gcprint(")");
        }
        else if( is(TK.SQRT) ) {
            gcprint("esqrt(");
            scan();
            expression();
            gcprint(")");
        }
        else if( is(TK.ID) ) {
            referenced_id(tok.string, false, tok.lineNumber);
            gcprintid(tok.string);
            scan();
        }
        else if( is(TK.NUM) ) {
            gcprint(tok.string);
            scan();
        }
 	else if( first(f_predef) ){
            predef();
	}
        else
            parse_error("factor");
    }

    // be careful: use lno here, not tok.lineNumber
    // (which may have been advanced by now)
    private void referenced_id(String id, boolean assigned, int lno) {
        Entry e = symtab.search(id);
        if( e == null) {
            System.err.println("undeclared variable "+ id + " on line " + lno);
            System.exit(1);
        }
        e.ref(assigned, lno);
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
        if( ! is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " + tok);
            parse_error( "missing token (mustbe)" );
        }
        scan();
    }
    boolean first(TK [] set) {
        int k = 0;
        while(set[k] != TK.none && set[k] != tok.kind) {
            k++;
        }
        return set[k] != TK.none;
    }

    private void parse_error(String msg) {
        System.err.println( "can't parse: line " + tok.lineNumber + " " + msg );
        System.exit(1);
    }
}
