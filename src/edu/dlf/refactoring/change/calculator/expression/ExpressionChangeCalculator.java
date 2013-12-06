package edu.dlf.refactoring.change.calculator.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

import com.google.inject.Inject;

import edu.dlf.refactoring.analyzers.ASTAnalyzer;
import edu.dlf.refactoring.analyzers.ASTNode2IntegerUtils;
import edu.dlf.refactoring.analyzers.FJUtils;
import edu.dlf.refactoring.change.ChangeBuilder;
import edu.dlf.refactoring.change.ChangeComponentInjector.AssignmentAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.CastAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.ExpressionAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.FieldAccessAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.InfixExpressionAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.MethodInvocationAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.NameAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.PrePostFixExpressionAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.ThisAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.VariableDeclarationAnnotation;
import edu.dlf.refactoring.change.IASTNodeChangeCalculator;
import edu.dlf.refactoring.design.ASTNodePair;
import edu.dlf.refactoring.design.ISourceChange;
import fj.Equal;
import fj.F;
import fj.F2;
import fj.P2;
import fj.data.List;

public class ExpressionChangeCalculator implements IASTNodeChangeCalculator{

	private final IASTNodeChangeCalculator vdCalculator;
	private final IASTNodeChangeCalculator asCalculator;
	private final ChangeBuilder changeBuilder;
	private final IASTNodeChangeCalculator nCalculator;
	private final IASTNodeChangeCalculator ppfCalculator;
	private final IASTNodeChangeCalculator infCalculator;
	private final IASTNodeChangeCalculator miCalculator;
	private final IASTNodeChangeCalculator fieldAccessCal;
	private final IASTNodeChangeCalculator castExpCal;
	private final IASTNodeChangeCalculator thisExpCal;

	@Inject
	public ExpressionChangeCalculator(
			@ExpressionAnnotation String changeLevel,
			@VariableDeclarationAnnotation IASTNodeChangeCalculator vdCalculator,			
			@AssignmentAnnotation IASTNodeChangeCalculator asCalculator,
			@NameAnnotation IASTNodeChangeCalculator nCalculator,
			@PrePostFixExpressionAnnotation IASTNodeChangeCalculator ppfCalculator,
			@InfixExpressionAnnotation IASTNodeChangeCalculator infCalculator,
			@MethodInvocationAnnotation IASTNodeChangeCalculator miCalculator,
			@FieldAccessAnnotation IASTNodeChangeCalculator fieldAccessCal,
			@ThisAnnotation IASTNodeChangeCalculator thisExpCal,
			@CastAnnotation IASTNodeChangeCalculator castExpCal) {
		this.asCalculator = asCalculator;
		this.vdCalculator = vdCalculator;
		this.nCalculator = nCalculator;
		this.ppfCalculator = ppfCalculator;
		this.infCalculator = infCalculator;
		this.miCalculator = miCalculator;
		this.fieldAccessCal = fieldAccessCal;
		this.thisExpCal = thisExpCal;
		this.castExpCal = castExpCal;
		this.changeBuilder = new ChangeBuilder(changeLevel);
	}
	
	private final F<ASTNode, List<ASTNode>> getDecExpressions = ASTAnalyzer.
		getAllDecendantsFunc.andThen(new F<List<ASTNode>, List<ASTNode>>() {
			@Override
			public List<ASTNode> f(List<ASTNode> decendants) {
				return decendants.filter(new F<ASTNode, Boolean>() {
					@Override
					public Boolean f(ASTNode node) {
						return node instanceof Expression;
			}});
	}});
	
	private final Equal<ASTNode> typeEq = Equal.intEqual.comap
		(ASTNode2IntegerUtils.getKind);
	
	private final Equal<List<ASTNode>> listTypeEq = typeEq.comap(FJUtils.
		getHeadFunc((ASTNode)null));
	
	private final F2<List<ASTNode>, List<ASTNode>, List<P2<ASTNode, ASTNode>>> 
		expMapper = ASTAnalyzer.getASTNodeMapper(6, ASTAnalyzer.
				getDefaultASTNodeSimilarityScore(10));
	
	
	
	@Override
	public ISourceChange CalculateASTNodeChange(ASTNodePair pair) {
		ISourceChange change = changeBuilder.buildSimpleChange(pair);
		if(change != null)
			return change;
		
		Expression expBefore, expAfter;
		expBefore = (Expression) pair.getNodeBefore();
		expAfter = (Expression) pair.getNodeAfter();
		
		if(expBefore.getNodeType() != expAfter.getNodeType())
		{
			List<List<ASTNode>> groupsBefore = getDecExpressions.f(expBefore).
				snoc(expBefore).group(typeEq);
			List<List<ASTNode>> groupsAfter = getDecExpressions.f(expAfter).
				snoc(expAfter).group(typeEq);
			List<P2<List<ASTNode>, List<ASTNode>>> groupPairs = FJUtils.
				getSamePairs(groupsBefore, groupsAfter, listTypeEq);
			List<P2<ASTNode, ASTNode>> pairs = groupPairs.bind(expMapper.tuple());
			
			return changeBuilder.createUnknownChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.ASSIGNMENT)
		{
			return this.asCalculator.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION)
		{
			return this.vdCalculator.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.SIMPLE_NAME || 
			expBefore.getNodeType() == ASTNode.QUALIFIED_NAME)
		{
			return this.nCalculator.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.PREFIX_EXPRESSION || 
				expBefore.getNodeType() == ASTNode.POSTFIX_EXPRESSION)
		{
			return this.ppfCalculator.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.INFIX_EXPRESSION)
		{
			return this.infCalculator.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.METHOD_INVOCATION)
		{
			return this.miCalculator.CalculateASTNodeChange(pair);
		}
		if(expBefore.getNodeType() == ASTNode.FIELD_ACCESS) 
		{
			return this.fieldAccessCal.CalculateASTNodeChange(pair);
		}
		if(expBefore.getNodeType() == ASTNode.THIS_EXPRESSION) 
		{
			return this.thisExpCal.CalculateASTNodeChange(pair);
		}
		
		if(expBefore.getNodeType() == ASTNode.CAST_EXPRESSION) 
		{
			return this.castExpCal.CalculateASTNodeChange(pair);
		}

		return changeBuilder.createUnknownChange(pair);
	}
	
	


}
