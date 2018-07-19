package com.paisheng.bi.util;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.paisheng.bi.bean.CheckPointBean;
import com.paisheng.bi.constant;

import java.io.IOException;
import java.util.List;

public class WriteToFile {
    public static void write(final AnActionEvent e, final PsiClass psiClass, final PsiMethod selectMethod, final String className, final List<CheckPointBean> list) {
        Project project = e.getProject();
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        // 创建文件系统
        PsiClass[] virtualFiles = createFile(e);
        // 写文件
        if (virtualFiles != null) {
            toWrite(project, editor, virtualFiles, psiClass, selectMethod, className, list);
        }
    }

    private static void toWrite(Project project, Editor editor, PsiClass[] psiClassesList, PsiClass psiClassPoint, PsiMethod psiMethodPoint, String annotationName, List<CheckPointBean> list) {
        writeAspect(project, editor, psiClassesList[0], psiClassPoint, psiMethodPoint, annotationName, list);
        writeNote(project, editor, psiClassesList[1], psiClassPoint, psiMethodPoint, annotationName, list);
    }

    private static void writeAspect(final Project project, final Editor editor, final PsiClass psiClass, final PsiClass psiClassPoint,
                                    final PsiMethod psiMethodPoint, final String annotationName, final List<CheckPointBean> list) {
        new WriteCommandAction.Simple(project, psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                new WriteAspectFile(project, editor, psiClass, psiClassPoint, psiMethodPoint, annotationName, list).run();
            }
        }.execute();
    }

    private static void writeNote(Project project, Editor editor, PsiClass psiClass, PsiClass psiClassPoint, PsiMethod psiMethodPoint, String annotationName, List<CheckPointBean> list) {
        new WriteCommandAction.Simple(project, psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                //do something
            }
        }.execute();
    }

    private static PsiClass[] createFile(AnActionEvent e) {
        Project project = e.getProject();
        Module mModule = e.getData(LangDataKeys.MODULE);
        if (project != null && mModule != null) {
            String ModuleName = mModule.getName();
            String src = ModuleName + "/src/bi/java/bi";
            try {
                // 获取virtualFileDirectory
                VirtualFile virtualFileDirectory = ProjectHelper.createFolderIfNotExist(project, src);
                // 获取biPsiDirectory
                PsiDirectory biPsiDirectory = PsiManager.getInstance(project).findDirectory(virtualFileDirectory);
                if (biPsiDirectory != null) {
                    // 获取类名
                    String[] classNames = getFormatName(ModuleName.replaceAll("[Bb]iz_", ""));
                    // 定义PsiClass数组
                    PsiClass[] PsiClassArray = new PsiClass[2];
                    // 查找PsiFile
                    PsiFile psiFile1 = biPsiDirectory.findFile(classNames[0]);
                    if (psiFile1 instanceof PsiJavaFile && ((PsiJavaFile) psiFile1).getClasses().length > 0) {
                        // 赋值
                        PsiClassArray[0] = ((PsiJavaFile) psiFile1).getClasses()[0];
                    } else {
                        // 创建
//                        PsiClassArray[0] = JavaDirectoryService.getInstance().createClass(biPsiDirectory, classNames[0].replace(".java", ""));
                        VirtualFile virtualFile = virtualFileDirectory.findOrCreateChildData(project, classNames[0]);
                        ProjectHelper.setFileContent(project, virtualFile, String.format(constant.ASPECT, classNames[0].replace(".java", "")));
                        PsiClassArray[0] = ((PsiJavaFile) PsiManager.getInstance(project).findFile(virtualFile)).getClasses()[0];
                    }
                    //  查找PsiFile
                    PsiFile psiFile2 = biPsiDirectory.findFile(classNames[1]);
                    if (psiFile2 instanceof PsiJavaFile && ((PsiJavaFile) psiFile2).getClasses().length > 0) {
                        // 赋值
                        PsiClassArray[1] = ((PsiJavaFile) psiFile2).getClasses()[0];
                    } else {
                        // 创建
//                        PsiClassArray[1] = JavaDirectoryService.getInstance().createAnnotationType(biPsiDirectory, classNames[1].replace(".java", ""));
                        VirtualFile virtualFile = virtualFileDirectory.findOrCreateChildData(project, classNames[1]);
                        ProjectHelper.setFileContent(project, virtualFile, String.format(constant.NOTE, classNames[1].replace(".java", "")));
                        PsiClassArray[1] = ((PsiJavaFile) PsiManager.getInstance(project).findFile(virtualFile)).getClasses()[0];
                    }
                    // 加入编辑器
                    FileEditorManager manager = FileEditorManager.getInstance(project);
                    manager.openFile(PsiClassArray[0].getContainingFile().getVirtualFile(), true, true);
                    manager.openFile(PsiClassArray[1].getContainingFile().getVirtualFile(), true, true);
                    return PsiClassArray;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    private static String[] getFormatName(String className) {
        String temp = formatNamed(className);
        return new String[]{"Bi" + temp + "Aspect.java", "Bi" + temp + "Note.java"};
    }

    private static String formatNamed(String name) {
        char[] ch = name.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }
}