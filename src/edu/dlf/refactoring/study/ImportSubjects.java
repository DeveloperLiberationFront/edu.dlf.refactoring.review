package edu.dlf.refactoring.study;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;

import com.google.inject.Inject;

import edu.dlf.refactoring.analyzers.FileUtils;
import edu.dlf.refactoring.design.IFactorComponent;
import edu.dlf.refactoring.design.ServiceLocator.ChangeCompAnnotation;
import edu.dlf.refactoring.utils.EclipseUtils;
import fj.F;
import fj.F2;
import fj.Ord;
import fj.data.List;

public class ImportSubjects extends AbstractStudy {

	private final Logger logger;
	private final String root;
	private final IFactorComponent changeComp;
	private static final String projectName = "junit";
	
	@Inject
	public ImportSubjects(
		Logger logger,
		@ChangeCompAnnotation IFactorComponent changeComp) {
		super(projectName + " study");
		this.logger = logger;
		this.root = FileUtils.desktop;
		this.changeComp = changeComp;
	}

	private final F2<String, Integer, List<IProject>> importAllProjects = 
		new F2<String, Integer, List<IProject>>() {
		@Override
		public List<IProject> f(final String path, final Integer count) {
			List<File> files = FileUtils.searchFilesInDirectory.f(path, 
				new F<File, Boolean>() {
				@Override
				public Boolean f(File file) {
					return file.getName().equals(".project") && !file.
						getAbsolutePath().contains("bin");
			}});
			List<IProject> importedProjects = files.map(FileUtils.getPath.
				andThen(EclipseUtils.importGetProject));	
			importedProjects.zip(importedProjects.map(EclipseUtils.getProjectName).
				map(new F<String, String>(){
				@Override
				public String f(String oldName) {
					return oldName + count;
				}})).foreach(EclipseUtils.directlyRenameProject.tuple());
			
			return importedProjects;
	}}; 
	
	@Override
	protected void study() {
		List<String> folders = FileUtils.getSubDirectories.f(root).filter
			(new F<String, Boolean>(){
			@Override
			public Boolean f(String path) {
				return path.contains(projectName);
		}}).sort(Ord.stringOrd);
		folders.zipIndex().bind(importAllProjects.tuple());
	}
}
