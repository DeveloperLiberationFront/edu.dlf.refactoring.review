package edu.dlf.refactoring.change.calculator;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import com.google.inject.Inject;

import edu.dlf.refactoring.analyzers.ASTAnalyzer;
import edu.dlf.refactoring.analyzers.ASTNode2ASTNodeUtils;
import edu.dlf.refactoring.analyzers.ASTNode2IntegerUtils;
import edu.dlf.refactoring.analyzers.ASTNode2StringUtils;
import edu.dlf.refactoring.analyzers.ASTNodeMapperUtils;
import edu.dlf.refactoring.analyzers.FJUtils;
import edu.dlf.refactoring.analyzers.JavaModelAnalyzer;
import edu.dlf.refactoring.change.ChangeBuilder;
import edu.dlf.refactoring.change.ChangeComponentInjector.AnnotationTypeDeclarationAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.CompilationUnitAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.EnumDeclarationAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.ImportDeclarationAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.NameAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.PackageDeclarationAnnotation;
import edu.dlf.refactoring.change.ChangeComponentInjector.TypeDeclarationAnnotation;
import edu.dlf.refactoring.change.IASTNodeChangeCalculator;
import edu.dlf.refactoring.change.IJavaModelChangeCalculator;
import edu.dlf.refactoring.change.SubChangeContainer;
import edu.dlf.refactoring.design.ASTNodePair;
import edu.dlf.refactoring.design.ISourceChange;
import edu.dlf.refactoring.design.ISourceChange.SourceChangeType;
import edu.dlf.refactoring.design.JavaElementPair;
import fj.Equal;
import fj.F;
import fj.F2;
import fj.P2;
import fj.data.List;
import fj.data.Option;


public class CompilationUnitChangeCalculator implements IJavaModelChangeCalculator
	, IASTNodeChangeCalculator{

	private final Logger logger;
	
	private final IASTNodeChangeCalculator typeChangeCalculator;
	private final IASTNodeChangeCalculator enumDeclarationCal;
	private final IASTNodeChangeCalculator annotationTypeCal;
	private final IASTNodeChangeCalculator nameChangeCal;
	
	private final ChangeBuilder compilationUnitCB;
	private final ChangeBuilder packageDeclarationCB;
	private final ChangeBuilder importDecalarationCB;	
	
	@Inject
	public CompilationUnitChangeCalculator(
			Logger logger,
			@CompilationUnitAnnotation String compilationUnitLV,
			@PackageDeclarationAnnotation String packageDeclarationLV,
			@ImportDeclarationAnnotation String importDeclarationLV,
			@NameAnnotation IASTNodeChangeCalculator nameChangeCal,
			@TypeDeclarationAnnotation IASTNodeChangeCalculator typeChangeCalculator,
			@EnumDeclarationAnnotation IASTNodeChangeCalculator enumDeclarationCal,
			@AnnotationTypeDeclarationAnnotation IASTNodeChangeCalculator annotationTypeCal) {
		this.logger = logger;
		this.typeChangeCalculator = typeChangeCalculator;
		this.enumDeclarationCal = enumDeclarationCal;
		this.annotationTypeCal = annotationTypeCal;
		this.nameChangeCal = nameChangeCal;
		this.compilationUnitCB = new ChangeBuilder(compilationUnitLV);
		this.packageDeclarationCB = new ChangeBuilder(packageDeclarationLV);
		this.importDecalarationCB = new ChangeBuilder(importDeclarationLV);
	}
	
	private final F<ASTNode, List<ASTNode>> getAbstractTypeFunc = 
		ASTNode2ASTNodeUtils.getStructuralPropertyFunc.flip().
			f(CompilationUnit.TYPES_PROPERTY);
	
	private final F<ASTNode, List<ASTNode>> getImportsFunc = ASTNode2ASTNodeUtils.
		getStructuralPropertyFunc.flip().f(CompilationUnit.IMPORTS_PROPERTY);
	
	private final Equal<ASTNode> nodeTypeEq = Equal.intEqual.comap(ASTNode2IntegerUtils.
		getKind);
	
	private final F<ASTNodePair, Option<ISourceChange>> calculateTypeDecChange = 
		new F<ASTNodePair, Option<ISourceChange>>() {
		@Override
		public Option<ISourceChange> f(ASTNodePair typePair) {
			int kind = typePair.getNodeBefore() != null ? typePair.getNodeBefore().
					getNodeType() : typePair.getNodeAfter().getNodeType();
			switch(kind) {
			case ASTNode.TYPE_DECLARATION:
				return Option.some(typeChangeCalculator.CalculateASTNodeChange
					(typePair));
			case ASTNode.ENUM_DECLARATION:	
				return Option.some(enumDeclarationCal.CalculateASTNodeChange
					(typePair));
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				return Option.some(annotationTypeCal.CalculateASTNodeChange
					(typePair));
			default: 
				logger.fatal("Unknown type declaration.");
				return Option.none();
	}}};
			
	@Override
	public ISourceChange calculate(JavaElementPair pair) {
		logger.debug("Calculate change: " + JavaModelAnalyzer.getElementNameFunc.
			f(pair.getElementBefore()) + "->" + JavaModelAnalyzer.getElementNameFunc.
				f(pair.getElementAfter()) );
		ASTNode cuBefore = ASTAnalyzer.parseICompilationUnit(pair.getElementBefore());
		ASTNode cuAfter = ASTAnalyzer.parseICompilationUnit(pair.getElementAfter());
		return CalculateASTNodeChange(new ASTNodePair(cuBefore, cuAfter));
	}
	
	@Override
	public ISourceChange CalculateASTNodeChange(ASTNodePair pair) {
		ISourceChange simple = compilationUnitCB.buildSimpleChange(pair);
		if(simple != null) {
			return breakSimpleChange(simple);
		}
		logger.info("Calculate change: " + ASTNode2StringUtils.getCompilationUnitName.
			f(pair.getNodeBefore()) + "->" + ASTNode2StringUtils.getCompilationUnitName.
				f(pair.getNodeAfter()));
		ASTNode cuBefore = pair.getNodeBefore();
		ASTNode cuAfter = pair.getNodeAfter();
		ASTNodePair aPair = new ASTNodePair(cuBefore, cuAfter);
		ISourceChange change = compilationUnitCB.buildSimpleChange(aPair);
		if(change != null)
			return change;
		try{	
			SubChangeContainer container = compilationUnitCB.createSubchangeContainer
				(pair);
			container.addSubChange(calculatePackageDeclarationChange(pair.
				selectByPropertyDescriptor(CompilationUnit.PACKAGE_PROPERTY)));
			container.addMultiSubChanges(calculateImportDeclarationsChange
				(getImportsFunc.f(pair.getNodeBefore()), getImportsFunc.
					f(pair.getNodeAfter())));
			List<ASTNode> typesBefore =  getAbstractTypeFunc.f(cuBefore);
			List<ASTNode> typesAfter = getAbstractTypeFunc.f(cuAfter);
			List<P2<List<ASTNode>, List<ASTNode>>> groups = FJUtils.
				pairEqualElements(typesBefore.group(nodeTypeEq), typesAfter.
					group(nodeTypeEq), Equal.intEqual.comap(FJUtils.getHeadFunc
						((ASTNode)null).andThen(ASTNode2IntegerUtils.getKind)));		
			F<P2<List<ASTNode>, List<ASTNode>>, List<ASTNodePair>> mapper = 
				ASTNodeMapperUtils.getASTNodeMapper(20, ASTNodeMapperUtils.
				getCommonWordsASTNodeSimilarityScoreFunc(100, new F<ASTNode, String>() {
				@Override
				public String f(ASTNode node) {
					return ((AbstractTypeDeclaration)node).getName().getIdentifier();
			}})).tuple().andThen(ASTNodePair.createPairFunc.tuple().mapList());
			Collection<ASTNodePair> nodePairs = groups.bind(mapper).toCollection();
			addSubChanges(container, nodePairs);
			return container;
		} catch(Exception e) {
			logger.fatal(e);
			return compilationUnitCB.createUnknownChange(aPair);
		}
	}

	private ISourceChange breakSimpleChange(ISourceChange simple) {
		if(simple.getSourceChangeType() == SourceChangeType.ADD) {
			simple.addMultiSubChanges(breakCompilationAdd(simple.getNodeAfter()));
		}
		if(simple.getSourceChangeType() == SourceChangeType.REMOVE) {
			simple.addMultiSubChanges(breakCompilationRemove(simple.getNodeBefore()));
		}
		return simple;
	}
	
	private Collection<ISourceChange> breakCompilationAdd(ASTNode unit) {
		SubChangeContainer container = compilationUnitCB.createSubchangeContainer
			(new ASTNodePair(null, unit));
		List<ASTNodePair> typePairs = getAbstractTypeFunc.f(unit).map(FJUtils.
			prependElementFunc((ASTNode)null, (ASTNode)null)).map(ASTNodePair.
				createPairFunc.tuple());
		return map2SourceChanges(typePairs);
	}
	
	private final F<Option<ISourceChange>, Boolean> isSome = FJUtils.getIsSome
		((ISourceChange)null);
	
	private final F<Option<ISourceChange>, ISourceChange> getSome = FJUtils.
		getGetSomeFunc((ISourceChange)null);
	
	private Collection<ISourceChange> breakCompilationRemove(ASTNode unit) {
		SubChangeContainer container = compilationUnitCB.createSubchangeContainer
			(new ASTNodePair(unit, null));
		List<ASTNodePair> typePairs = getAbstractTypeFunc.f(unit).map(FJUtils.
			appendElementFunc((ASTNode)null, (ASTNode)null)).map(ASTNodePair.
				createPairFunc.tuple());
		return map2SourceChanges(typePairs);
	}

	private Collection<ISourceChange> map2SourceChanges(List<ASTNodePair> typePairs) {
		return typePairs.map(this.calculateTypeDecChange).filter(isSome).map
			(getSome).toCollection();
	}
	
	private Collection<ISourceChange> calculateImportDeclarationsChange(List<ASTNode> 
		importsBefore, List<ASTNode> importsAfter) {
		F2<List<ASTNode>, List<ASTNode>, List<P2<ASTNode, ASTNode>>> mapper = 
			ASTNodeMapperUtils.getASTNodeMapper(20, ASTNodeMapperUtils.
			getASTNodeSimilarityFunc(100, new F<ASTNode, String>() {
				@Override
				public String f(ASTNode importDec) {
					return importDec.getStructuralProperty(ImportDeclaration.
						NAME_PROPERTY).toString();
		}}));
		List<ASTNodePair> importPairs = mapper.f(importsBefore, importsAfter).
			map(ASTNodePair.createPairFunc.tuple());
		return importPairs.map(calculateImportChange).toCollection();
	}
	
	private F<ASTNodePair, ISourceChange> calculateImportChange = new F<ASTNodePair, 
		ISourceChange>() {
		@Override
		public ISourceChange f(ASTNodePair pair) {
			ISourceChange change = importDecalarationCB.buildSimpleChange(pair);
			if(change != null)
				return change;
			SubChangeContainer container = importDecalarationCB.
				createSubchangeContainer(pair);
			container.addSubChange(nameChangeCal.CalculateASTNodeChange(pair.
				selectByPropertyDescriptor(ImportDeclaration.NAME_PROPERTY)));
			return container;
	}};
	
	private ISourceChange calculatePackageDeclarationChange(ASTNodePair pair) {
		ISourceChange change = packageDeclarationCB.buildSimpleChange(pair);
		if(change != null)
			return change;
		SubChangeContainer container = packageDeclarationCB.
			createSubchangeContainer(pair);
		ASTNodePair namePair = pair.selectByPropertyDescriptor
			(PackageDeclaration.NAME_PROPERTY);
		container.addSubChange(nameChangeCal.CalculateASTNodeChange(namePair));
		return container;
	}

	private void addSubChanges(SubChangeContainer container, Collection
			<ASTNodePair> nodePairs) throws Exception {
		for(ASTNodePair typePair : nodePairs) {
			Option<ISourceChange> op = calculateTypeDecChange.f(typePair);
			if(op.isSome())
				container.addSubChange(op.some());
		}
	}
}
