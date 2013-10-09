package edu.dlf.refactoring.detectors;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.inject.Inject;

import edu.dlf.refactoring.analyzers.ASTAnalyzer;
import edu.dlf.refactoring.change.ChangeComponentInjector.CompilationUnitAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.MethodDeclarationAnnotation;
import edu.dlf.refactoring.change.SourceChangeUtils;
import edu.dlf.refactoring.design.IDetectedRefactoring;
import edu.dlf.refactoring.design.ISourceChange;
import edu.dlf.refactoring.design.RefactoringType;
import edu.dlf.refactoring.design.ISourceChange.SourceChangeType;
import edu.dlf.refactoring.detectors.SourceChangeSearcher.IChangeSearchCriteria;
import edu.dlf.refactoring.detectors.SourceChangeSearcher.IChangeSearchResult;
import edu.dlf.refactoring.refactorings.DetectedMoveRefactoring;
import fj.F;
import fj.P2;
import fj.data.List;
import fj.data.List.Buffer;

public class MoveDetector extends AbstractRefactoringDetector{

	private final Logger logger;
	private final String cuLevel;
	private final CascadeChangeCriteriaBuilder builder;
	private final String mdLevel;

	@Inject
	public MoveDetector(Logger logger,
			CascadeChangeCriteriaBuilder builder,
			@CompilationUnitAnnotation String cuLevel,
			@MethodDeclarationAnnotation String mdLevel)
	{
		this.logger = logger;
		this.cuLevel = cuLevel;
		this.mdLevel = mdLevel;
		this.builder = builder;
	}
	
	@Override
	public List<IDetectedRefactoring> detectRefactoring(ISourceChange change) {
		final List<ISourceChange> cuChanges = SourceChangeUtils.getSelfAndDescendent
			(change).filter(new F<ISourceChange, Boolean>() {
			@Override
			public Boolean f(ISourceChange child) {
				return child.getSourceChangeLevel() == cuLevel;
			}
		});
	
		final F<String, IChangeSearchCriteria> getAddCriteria = new F<String, 
			IChangeSearchCriteria>(){
			@Override
			public IChangeSearchCriteria f(String level) {
				builder.reset();
				return builder.addSingleChangeCriteria
					(level, SourceChangeType.ADD).getSearchCriteria();
			}};

		final F<String, IChangeSearchCriteria> getRemoveCriteria = new F<String, 
			IChangeSearchCriteria>(){
			@Override
			public IChangeSearchCriteria f(String level) {
				builder.reset();
				return builder.addSingleChangeCriteria
					(level, SourceChangeType.REMOVE).getSearchCriteria();
			}};
			
		final F<IChangeSearchCriteria, List<ISourceChange>> getLowestChanges = 
			new F<IChangeSearchCriteria, List<ISourceChange>>(){
			@Override
			public List<ISourceChange> f(final IChangeSearchCriteria criteria) {
				return cuChanges.bind(new F<ISourceChange, List<ISourceChange>>() {
					@Override
					public List<ISourceChange> f(ISourceChange change) {
						List<IChangeSearchResult> results = criteria.search(change);
						return results.map(new F<IChangeSearchResult, ISourceChange>() {
							@Override
							public ISourceChange f(IChangeSearchResult result) {
								return result.getSourceChanges().reverse().head();
							}});}});}};
	
		final F<ISourceChange, ASTNode> getBeforeNode = new F<ISourceChange, ASTNode>() {
			@Override
			public ASTNode f(ISourceChange change) {
				return change.getNodeBefore();
			}
		};
		
		final F<ISourceChange, ASTNode> getAfterNode = new F<ISourceChange, ASTNode>() {
			@Override
			public ASTNode f(ISourceChange change) {
				return change.getNodeBefore();
			}
		};
		Buffer<IDetectedRefactoring> allRefactorings = Buffer.empty();
		List<ASTNode> addedMethods = getLowestChanges.f(getAddCriteria.f(mdLevel)).
			map(getAfterNode);
		List<ASTNode> removedMethods = getLowestChanges.f(getRemoveCriteria.f(mdLevel)).
			map(getBeforeNode);
		allRefactorings.append(ASTAnalyzer.getSameNodePairs(removedMethods, 
			addedMethods, ASTAnalyzer.getMethodDeclarationNamesEqualFunc()).map
				(new F<P2<ASTNode, ASTNode>, IDetectedRefactoring>() {
				@Override
				public IDetectedRefactoring f(P2<ASTNode, ASTNode> p) {
					return new DetectedMoveRefactoring(RefactoringType.
						MoveMethod, p._1(), p._2());
				}
			}));
		return allRefactorings.toList();
	}

}
