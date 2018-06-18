import java.io.FileInputStream;
import java.io.IOException;

//import mypackage.LuaParser.LabelContext;
//import LuaParser.*;

//import org.antlr.v4.runtime.ANTLRInputStream;
//import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
//import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.*;

/**
 * @author Leonardo Kenji Feb 4, 2014
 */
public class MyVisitor extends LuaBaseVisitor<String> {

    /**
     * Main Method
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        //ANTLRInputStream input = new ANTLRInputStream(new FileInputStream("/home/sergio/Dropbox/errorrecovery/antlr/ast/teste.lua")); 
        if (args.length < 1) {
            System.out.println("Usage: java MyVisitor InputFile");
            return;
        }

        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(args[0])); 
        LuaLexer lexer = new LuaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LuaParser parser = new LuaParser(tokens);
        LuaParser.ChunkContext tree = parser.chunk(); // see the grammar ->
                                                    // starting point for
                                                    // parsing a java file



        MyVisitor visitor = new MyVisitor(); // extends LuaBaseVisitor<Void>
                                                // and overrides the methods
                                                // you're interested

        String res = visitor.visit(tree);
        System.out.println("AST: ");
        System.out.println(res);
    }

    @Override
    public String visitChunk(LuaParser.ChunkContext ctx) {
        return visitBlock(ctx.block());
    }

    @Override
    public String visitBlock(LuaParser.BlockContext ctx) {
        if (ctx == null)
            return "{ nullBlock }";
        String tree = "";
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (i > 0)
                tree += ", ";
            if (ctx.getChild(i) instanceof LuaParser.StatContext) {
                tree += "{" + visitStat(ctx.stat(i)) + "}";
            } else {
                tree += visitRetstat(ctx.retstat());
            }
        }
        return tree;
    }

    @Override
    public String visitStat(LuaParser.StatContext ctx) {
        //System.out.println("Stat name:" + ctx.getText());
        if (ctx.getChildCount() == 0)
            return ":StatNoChild ";
        String tk = ctx.getChild(0).getText();
        if (ctx.varlist() != null) {
            return ":Set " + visitVarlist(ctx.varlist()) + ", " + visitExplist(ctx.explist());
        } else if (ctx.functioncall() != null) {
            return visitFunctioncall(ctx.functioncall());
        } else if (ctx.label() != null) {
            return visitLabel(ctx.label());
        } else if (tk.equals("break")) {
            return ":Break";
        } else if (tk.equals("goto")) {
            return ":Goto " + ctx.getChild(1).getText();
        } else if (tk.equals("do")) {
            return ":Do " + visitBlock(ctx.block(0));
        } else if (tk.equals("while")) {
            return ":While " + visitExp(ctx.exp(0)) + visitBlock(ctx.block(0));
        } else if (tk.equals("repeat")) {
            return ":Repeat " + visitBlock(ctx.block(0)) + visitExp(ctx.exp(0));
        } else if (tk.equals("if")) {
            String tree = ":If " + visitExp(ctx.exp(0)) + visitBlock(ctx.block(0));
            int i = 1;
            while (ctx.block(i) != null) {
                tree += ", ";
                if (ctx.exp(i) != null)
                    tree += visitExp(ctx.exp(i));
                tree += visitBlock(ctx.block(i));
                i++;
            }
            return tree;
        } else if (tk.equals("for")) {
            String tree = ":For ";
            if (ctx.namelist() != null) { //generic for
                tree += visitNamelist(ctx.namelist());
                tree += ", " + visitExplist(ctx.explist());
            } else if (ctx.exp(0) != null && ctx.exp(1) != null) { //numeric for
                System.out.println("For " + ctx.getChildCount() + " " + ctx.getText());
                tree += " {:Id " + ctx.getChild(1).getText() + "}, ";
                tree += visitExp(ctx.exp(0)) + ", " + visitExp(ctx.exp(1));
                if (ctx.exp(2) != null)
                    tree += ", " + visitExp(ctx.exp(2)); 
            } else {
                tree += " InvalidFor";
            }
            return tree + ", " + visitBlock(ctx.block(0));
        } else if (tk.equals("function")) {
            return ":Function " + visitFuncname(ctx.funcname()) + ", " + visitFuncbody(ctx.funcbody());
        } else if (tk.equals("local")) {
            if (ctx.namelist() != null) {
                String tree = ":Set " + visitNamelist(ctx.namelist());
                if (ctx.explist() != null)
                    tree += visitExplist(ctx.explist());
                return tree;
            } else {
                String tree = ":Function {:Funcname ";
                if (ctx.getChildCount() < 3)
                    tree += "NoName";
                else
                    tree += ctx.getChild(2).getText();
                return tree + ", " + visitFuncbody(ctx.funcbody());
            }
        } 
        else 
            return ":NotStat " + ctx.getText();
        //return "ASTNotImplemented";
    }

    @Override
    public String visitRetstat(LuaParser.RetstatContext ctx) {
        if (ctx.explist() == null)
            return ":RetStat {}";
        else 
            return ":RetStat {" + ctx.getChild(1).getText() + "}";
    }    

    @Override
    public String visitLabel(LuaParser.LabelContext ctx) {
        //System.out.println("LabelName name:" + ctx.getText() + ctx.getChildCount());
        return ":Label {" + ctx.getChild(1).getText();
    }

   @Override
    public String visitFuncname(LuaParser.FuncnameContext ctx) {
        String tree = ":Funcname " + (ctx.getChild(0) != null ? ctx.getChild(0).getText() : "FuncNoName");
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            tree += ", " + ctx.getChild(i).getText();
        }
        return "{" + tree + "}";
    }      

    @Override
    public String visitVarlist(LuaParser.VarlistContext ctx) {
        String tree = ":Varlist " + ctx.getChild(0).getText();
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            tree += ", " + ctx.getChild(i).getText();
        }
        return "{" + tree + "}";
    }

   @Override
    public String visitNamelist(LuaParser.NamelistContext ctx) {
        String tree = ":Namelist " + ctx.getChild(0).getText();
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            tree += ", " + ctx.getChild(i).getText();
        }
        return "{" + tree + "}";
    }

    @Override
    public String visitExplist(LuaParser.ExplistContext ctx) {
        String tree = ":Explist ";
        if (ctx == null)
            return "{" + tree + "}";
        tree = ":Explist " + visitExp(ctx.exp(0));
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            tree += ", " + visitExp(ctx.exp(i/2));
        }
        return "{" + tree + "}";
    }     


    @Override
    public String visitExp(LuaParser.ExpContext ctx) {
        if (ctx == null)
            return ":DummyExp";

        if (ctx.getChildCount() == 0)
            return "ExpNoChild";

        String exp1 = null, exp2 = null;
        if (ctx.exp(0) != null)
            exp1 = visitExp(ctx.exp(0));
        if (ctx.exp(1) != null)
            exp2 = visitExp(ctx.exp(1));

        String tk = ctx.getChild(0).getText();

        if (tk.equals("nil"))
            return ":Nil";
        else if (tk.equals("false"))
            return ":False";
        else if (tk.equals("true"))
            return "True";
        else if (ctx.number() != null)
            return ":Number{" + ctx.getText() + "}";
        else if (ctx.string() != null)
            return ":String{" + ctx.getText() + "}";
        else if (tk.equals("..."))
            return ":Dots";
        else if (ctx.functiondef() !=null)
            return visitFunctiondef(ctx.functiondef());
        else if (ctx.prefixexp() != null) 
            return visitPrefixexp(ctx.prefixexp());
        else if (ctx.tableconstructor() != null)
            return visitTableconstructor(ctx.tableconstructor());
        else if (ctx.operatorPower() != null) 
            return ":Op {" + ctx.operatorPower().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorUnary() != null)
            return ":Op {" + ctx.operatorUnary().getText() + " " + exp1 + "}";
        else if (ctx.operatorMulDivMod() != null)
            return ":Op {" + ctx.operatorMulDivMod().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorAddSub() != null)
            return ":Op {" + ctx.operatorAddSub().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorStrcat() != null)
            return ":Op {" + ctx.operatorStrcat().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorComparison() != null)
            return ":Op {" + ctx.operatorComparison().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorAnd() != null)
            return ":Op {" + ctx.operatorAnd().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorOr() != null)
            return ":Op {" + ctx.operatorOr().getText() + " " + exp1 + " " + exp2 + "}";
        else if (ctx.operatorBitwise() != null)
            return ":Op {" + ctx.operatorBitwise().getText() + " " + exp1 + " " + exp2 + "}";
        else    
            return "4242";
    }    


    @Override
    public String visitPrefixexp(LuaParser.PrefixexpContext ctx) {
        String tree = visitVarOrExp(ctx.varOrExp());
        for (int i = 0; i < ctx.getChildCount()-1; i++) {
            tree += ", " + visitNameAndArgs(ctx.nameAndArgs(i));
        }
        return tree;
    }


    @Override
    public String visitFunctioncall(LuaParser.FunctioncallContext ctx) {
        String tree = ":Functioncall " + visitVarOrExp(ctx.varOrExp());
        for (int i = 0; i < ctx.getChildCount() - 1; i++) {
            tree += ", " + visitNameAndArgs(ctx.nameAndArgs(i));
        }
        return "{" + tree + "}";
    } 

    @Override
    public String visitVarOrExp(LuaParser.VarOrExpContext ctx) {
        if (ctx.var() != null)
            return visitVar(ctx.var());
        else if (ctx.exp() != null)
            return visitExp(ctx.exp());
        else
            return "dummyVarOrExp";
    }

    @Override
    public String visitVar(LuaParser.VarContext ctx) {
        String tree = "";
        if (ctx.exp() == null)
            tree += ":Id " + ctx.getChild(0).getText();
        else 
            tree += ":Index " + visitExp(ctx.exp());

        for (int i = 0; ctx.varSuffix(i) != null; i++) {
            tree += ", " + visitVarSuffix(ctx.varSuffix(i));;
        }
        return "{" + tree + "}";
    }


    @Override
    public String visitVarSuffix(LuaParser.VarSuffixContext ctx) {
        String tree = "";
        for (int i = 0; ctx.nameAndArgs(i) != null; i++) {
            tree += ", " + visitNameAndArgs(ctx.nameAndArgs(i));;
        }
        if (ctx.exp() != null)
            return tree + visitExp(ctx.exp());
        else
            return tree + ctx.getChild(1).getText();
    }

   @Override
    public String visitNameAndArgs(LuaParser.NameAndArgsContext ctx) {
        String tree = "";
        if (ctx == null)
            return tree;
        if (ctx.getChildCount() > 1)
            tree += ctx.getChild(1).getText() + ", ";
        return tree + visitArgs(ctx.args());
    }

    @Override
    public String visitArgs(LuaParser.ArgsContext ctx) {
        if (ctx == null)
            return "nullVisitArgs";
        if (ctx.string() != null)
            return visitString(ctx.string());
        else if (ctx.tableconstructor() != null) {
            return visit(ctx.tableconstructor());
        }
        else
            return visitExplist(ctx.explist());
    }

    @Override
    public String visitFunctiondef(LuaParser.FunctiondefContext ctx) {
        if (ctx.funcbody() == null)
            return "{ :Function Empty}";
        else
            return "{ :Function " + visitFuncbody(ctx.funcbody()) + "}";
    }    

    @Override
    public String visitFuncbody(LuaParser.FuncbodyContext ctx) {
        if (ctx == null)
            return "EmptyFuncBody";
        return visitParlist(ctx.parlist()) + ", " + visitBlock(ctx.block());
    }   

    @Override
    public String visitParlist(LuaParser.ParlistContext ctx) {
        String tree = ":Parlist ";
        if (ctx == null)
            return "{" + tree + "}";
        else if (ctx.namelist() != null) {
            tree += visitNamelist(ctx.namelist());
            if (ctx.getChild(2) != null)
                tree += ", ...";
        } else
            tree += "...";

        return "{" + tree + "}";
    }   

    @Override
    public String visitTableconstructor(LuaParser.TableconstructorContext ctx) {
        if (ctx.fieldlist() != null)
            return visitFieldlist(ctx.fieldlist());
        return "";
    }   

    @Override
    public String visitFieldlist(LuaParser.FieldlistContext ctx) {
        String tree = visitField(ctx.field(0));
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            tree += visitField(ctx.field(i/2));
        }
        return tree;
    } 


    @Override
    public String visitField(LuaParser.FieldContext ctx) {
        String treeexp = visitExp(ctx.exp(0));
        if (ctx.getChildCount() == 1)
            return ":Id {" + treeexp + "}";
        else if (ctx.getChildCount() == 3)
            return ":Index {" + treeexp + " " + visitExp(ctx.exp(1)) + "}";
        else
            return ":Set {" + ctx.getChild(0).getText() + " " + treeexp + "}";
    }    


}
