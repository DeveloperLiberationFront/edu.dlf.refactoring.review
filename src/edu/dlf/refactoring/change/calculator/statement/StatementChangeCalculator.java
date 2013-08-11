package edu.dlf.refactoring.change.calculator.statement;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;

import com.google.inject.Inject;

import edu.dlf.refactoring.change.ChangeComponentInjector.BlockAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.DoStatementAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.ExpressionAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.ForStatementAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.IfStatementAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.StatementAnnotation;
import edu.dlf.refactoring.change.ChangeBuilder;
import edu.dlf.refactoring.change.ChangeComponentInjector.TryStatementAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.WhileStatementAnnotation;
import edu.dlf.refactoring.change.IASTNodeChangeCalculator;
import edu.dlf.refactoring.change.SubChangeContainer;
import edu.dlf.refactoring.design.ASTNodePair;
import edu.dlf.refactoring.design.ISourceChange;

public class StatementChangeCalculator implements IASTNodeChangeCalculator {

	private final IASTNodeChangeCalculator blockCalculator;
	private final IASTNodeChangeCalculator ifCalculator;
	private final IASTNodeChangeCalculator expressionCalculator;
	private final IASTNodeChangeCalculator fsCalculator;
	private final ChangeBuilder changeBuilder;
	private final IASTNodeChangeCalculator dsCalculator;
	private final IASTNodeChangeCalculator wsCalculator;
	private final IASTNodeChangeCalculator tsCalculator;
	
	@Inject
	public StatementChangeCalculator(
			@StatementAnnotation String changeLevel,
			@IfStatementAnnotation IASTNodeChangeCalculator ifCalculator,
			@ForStatementAnnotation IASTNodeChangeCalculator fsCalculator,
			@DoStatementAnnotation IASTNodeChangeCalculator dsCalculator,
			@WhileStatementAnnotation IASTNodeChangeCalculator wsCalculator,
			@TryStatementAnnotation IASTNodeChangeCalculator tsCalculator,
			@BlockAnnotation IASTNodeChangeCalculator blockCalculator, 
			@ExpressionAnnotation IASTNodeChangeCalculator expressionCalculator) {
		this.ifCalculator = ifCalculator;
		this.blockCalculator = blockCalculator;
		this.expressionCalculator = expressionCalculator;
		this.fsCalculator = fsCalculator;
		this.wsCalculator = wsCalculator;
		this.dsCalculator = dsCalculator;
		this.tsCalculator = tsCalculator;
		this.changeBuilder = new ChangeBuilder(changeLevel);
	}

	@Override
	public ISourceChange CalculateASTNodeChange(ASTNodePair pair) {
		
		ISourceChange change = this.changeBuilder.buildSimpleChange(pair);
		if(change != null)
			return change;
		
		if(pair.getNodeBefore().getNodeType() != pair.getNodeAfter().getNodeType())
			return this.changeBuilder.createUnknownChange(pair);
		
		switch(pair.getNodeBefore().getNodeType())
		{
		case ASTNode.IF_STATEMENT:
			return ifCalculator.CalculateASTNodeChange(pair);
		case ASTNode.BLOCK:
			return blockCalculator.CalculateASTNodeChange(pair);
		case ASTNode.FOR_STATEMENT:
			return fsCalculator.CalculateASTNodeChange(pair);
		case ASTNode.WHILE_STATEMENT:
			return wsCalculator.CalculateASTNodeChange(pair);
		case ASTNode.DO_STATEMENT:
			return dsCalculator.CalculateASTNodeChange(pair);
		case ASTNode.TRY_STATEMENT:
			return tsCalculator.CalculateASTNodeChange(pair);
		case ASTNode.EXPRESSION_STATEMENT:
			SubChangeContainer container = this.changeBuilder.createSubchangeContainer();
			container.addSubChange(expressionCalculator.CalculateASTNodeChange(new ASTNodePair(
				(ASTNode)pair.getNodeBefore().getStructuralProperty(ExpressionStatement.EXPRESSION_PROPERTY), 
				(ASTNode)pair.getNodeAfter().getStructuralProperty(ExpressionStatement.EXPRESSION_PROPERTY))));
			return container;
		default:
			return changeBuilder.createUnknownChange(pair);
		}
	}



}
