package com.paisheng.bi.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.paisheng.bi.bean.CheckPointBean;
import com.paisheng.bi.constant;

import java.util.List;

public class WriteAspectFile {
    private Project project;
    private PsiClass psiClass;//需要写入的Aspect类
    private PsiElementFactory psiElementFactory;
    private PsiClass psiClassPoint;//需要添加注解的类
    private PsiMethod psiMethodPoint;//需要添加注解的类的方法
    private String annotationName;//注解类的名称
    private List<CheckPointBean> list;//参数；
    private String noteName;//对应Note类名称

    public WriteAspectFile(Project project, PsiClass psiClass, PsiClass psiClassPoint, PsiMethod psiMethodPoint, String annotationName, List<CheckPointBean> list, String noteName) {
        this.project = project;
        this.psiClass = psiClass;
        psiElementFactory = JavaPsiFacade.getElementFactory(project);
        this.psiClassPoint = psiClassPoint;
        this.psiMethodPoint = psiMethodPoint;
        this.annotationName = annotationName;
        this.list = list;
        this.noteName = noteName;
    }

    public void run() {
        try {
            start();
            for (CheckPointBean item : list) {
                if (item.getPointType() == 1) {
                    sensors(item);
                } else if (item.getPointType() == 2) {
                    um(item);
                } else if (item.getPointType() == 3) {
                    local(item);
                }
            }
            openFiles(project, psiClass);
        } catch (Exception e) {
            Messages.showInfoMessage(e.toString(), "错误");
        }
    }

    private void openFiles(Project project, PsiClass... psiClasses) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (PsiClass psiClass :
                psiClasses) {
            fileEditorManager.openFile(psiClass.getContainingFile().getVirtualFile(), true, true);
        }
    }

    private void start() {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            if (!modifierList.hasModifierProperty(PsiKeyword.PUBLIC)) {
                modifierList.add(psiElementFactory.createKeyword(PsiKeyword.PUBLIC));
            }
        }
    }

    private void sensors(CheckPointBean item) {
        String tag = "Sensors";
        PsiClass psiClassSensors = psiClass.findInnerClassByName(tag, true);
        boolean isWillBeAdd = false;
        if (psiClassSensors == null) {
            psiClassSensors = psiElementFactory.createClassFromText(constant.SENSORS_ASPECT, null).getInnerClasses()[0];
            isWillBeAdd = true;
        }
        writeMethod(item, tag, psiClassSensors, constant.ASPECT_SENSORS_METHOD, isWillBeAdd);
    }


    private void um(CheckPointBean item) {
        String tag = "Um";
        PsiClass psiClassUm = psiClass.findInnerClassByName(tag, true);
        boolean isWillBeAdd = false;
        if (psiClassUm == null) {
            psiClassUm = psiElementFactory.createClassFromText(constant.UM_ASPECT, null).getInnerClasses()[0];
            isWillBeAdd = true;
        }
        writeMethod(item, tag, psiClassUm, constant.ASPECT_UM_METHOD, isWillBeAdd);
    }

    private void local(CheckPointBean item) {
        String tag = "Local";
        PsiClass psiClassLocal = psiClass.findInnerClassByName(tag, true);
        boolean isWillBeAdd = false;
        if (psiClassLocal == null) {
            psiClassLocal = psiElementFactory.createClassFromText(constant.LOCAL_ASPECT, null).getInnerClasses()[0];
            isWillBeAdd = true;
        }
        writeMethod(item, tag, psiClassLocal, constant.ASPECT_LOCAL_METHOD, isWillBeAdd);
    }

    private void writeMethod(CheckPointBean item, String tag, PsiClass psiClassParent, String format, boolean isWillBeAdd) {
        String AnnotationName = noteName + "$" + tag + "$" + annotationName;
        String methodName = formatNamedD(annotationName);
        StringBuilder checkedStr = new StringBuilder();
        StringBuilder values = new StringBuilder();
        JvmParameter[] parameters = psiMethodPoint.getParameters();
        List<Integer> parameterList = item.getParameterList();
        boolean isHasGetParameter = false;
        for (int i = 0; i < parameterList.size(); i++) {
            Integer type = parameterList.get(i);
            if (type == 0) {
                continue;
            }
            if (i == 0) {
                if (psiClassPoint.getName() != null && psiClassPoint.getName().length() > 0) {
                    checkedStr.append("checked = checked && joinPoint.getThis() instanceof " + psiClassPoint.getName() + ";\n");
                    values.append(psiClassPoint.getName() + " m" + formatNamedD(psiClassPoint.getName()) + " = " + "(" + psiClassPoint.getName() + ")joinPoint.getThis();\n");
                }
            } else {
                isHasGetParameter = true;
                String classPaName = getParameterClassName(parameters[i - 1]);
                // List需要做特殊处理
                String TypeName = classPaName.contains("List<") ? "List" : classPaName;
                if (type == 1) {
                    checkedStr.append("checked = checked && joinPoint.getArgs()[" + (i - 1) + "] instanceof " + TypeName + ";\n");
                } else {
                    checkedStr.append("checked = checked && Tools.checkTypeNullAble(joinPoint.getArgs()[" + (i - 1) + "], " + TypeName + ".class);\n");
                }
                values.append(classPaName + " " + parameters[i - 1].getName() + " = " + "(" + classPaName + ")joinPoint.getArgs()[" + (i - 1) + "];\n");
            }
        }
        if (isHasGetParameter) {
            checkedStr.insert(0, "checked = checked && joinPoint.getArgs() != null && joinPoint.getArgs().length == " + parameters.length + ";\n");
        }

        String methodStr = String.format(format, AnnotationName, methodName, checkedStr.toString(), values.toString());
        psiClassParent.add(psiElementFactory.createClassFromText(methodStr, null).getAllMethods()[0]);
        if (isWillBeAdd) {
            psiClass.add(psiClassParent);
        }
    }

    private String getParameterClassName(JvmParameter jvmParameter) {
        return basicTypesToPackagingType(jvmParameter.getType().toString().split(":")[1]);
    }

    private String basicTypesToPackagingType(String className) {
        if ("byte".equals(className)) {
            return "Byte";
        } else if ("short".equals(className)) {
            return "Short";
        } else if ("int".equals(className)) {
            return "Integer";
        } else if ("long".equals(className)) {
            return "Long";
        } else if ("float".equals(className)) {
            return "Float";
        } else if ("double".equals(className)) {
            return "Double";
        } else if ("boolean".equals(className)) {
            return "Boolean";
        } else if ("char".equals(className)) {
            return "Character";
        } else {
            return className;
        }
    }

    private static String formatNamedD(String name) {
        char[] ch = name.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }
}
