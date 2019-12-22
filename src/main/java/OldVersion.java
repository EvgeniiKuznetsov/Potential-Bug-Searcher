import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class OldVersion {
    public static void main(String[] args) throws Exception {
//        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\selfProduce\\.git"));
//        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\ktor22\\.git"));
//        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\kotlin-native21\\.git"));
//        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\kotlin2222222\\.git"));
//        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\Drill4J\\.git"));

        /** Открываем нужный нам гит репозиторий */
        Git git = Git.open(new File("C:\\Users\\Silent\\IdeaProjects\\find-bottleneck\\.git"));

        /** Достаём все логи (коммиты) */
        Iterable<RevCommit> logs = git.log().call();
        List<RevCommit> commits = new ArrayList<>();
        logs.forEach(commits::add);
        /** Переворачиваем список, ибо нам надо идти от самого старого к самому новому, а не наоборот */
        Collections.reverse(commits);
        Map<String, Integer> files = new HashMap<>();
        /** Пробегаем по всем коммитам */
        for (RevCommit commit : commits) {
            String commitID = commit.getName();
//            System.out.println(commitID);
            /** Проверяем существует ли коммит */
            if (commitID != null && !commitID.isEmpty()) {
                /** Снова достаём все логи */
                LogCommand logs2 = git.log().all();
                Repository repository = logs2.getRepository();
                /** То, что поможет нам бегать по ревизиям */
                RevWalk rw = new RevWalk(repository);
                /** Проверка на родителя */
                if (commit.getParents().length > 0) {
                    /** Достаём первого родителя (дальше нам не нужно, иначе статистика собьётся) */
                    RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
//                    System.out.println("Parent commit: " + parent.getName());
                    DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                    df.setRepository(repository);
                    df.setDiffComparator(RawTextComparator.DEFAULT);
                    df.setDetectRenames(true);
                    /** Получаем все дифы родителя и коммита */
                    List<DiffEntry> diffs = df.scan(commit.getTree(), parent.getTree());
                    for (DiffEntry diff : diffs) {
                        /** Отсеиваем ненужные расширения файлов */
//                        String[] filePath = diff.getNewPath().split("\\.");
//                        if (filePath.length >= 2 && (filePath[1].equals("kt") || filePath[1].equalsIgnoreCase("java"))) {
                            /** Исключаем добавление и удаление файлов, так как оно не нужно */
                            if (!(diff.getChangeType().name().equalsIgnoreCase("ADD") || diff.getChangeType().name().equalsIgnoreCase("DELETE"))) {
                                /** Отслеживаем изменение файлов. Если было сделано переименование или перемещение, то меняем путь файла или его название */
                                if(diff.getChangeType().name().equalsIgnoreCase("RENAME") || diff.getChangeType().name().equalsIgnoreCase("COPY")){
                                    if(files.containsKey(diff.getOldPath())){
                                        Integer count= files.get(diff.getOldPath()) + 1;
                                        files.remove(diff.getOldPath());
                                        files.put(diff.getNewPath(), count);
                                    } else {
                                        files.put(diff.getNewPath(), 1);
                                    }
                                } else {
                                    if (files.containsKey(diff.getNewPath())) {
                                        Integer count = files.get(diff.getNewPath()) + 1;
                                        files.replace(diff.getNewPath(), files.get(diff.getNewPath()), count);
//                                System.out.println("key/value: " + diff.getNewPath() + "/" + files.get(diff.getNewPath()));
                                    } else {
                                        files.put(diff.getNewPath(), 1);
                                    }
                                }
//                            }
                        }
                    }
                }
            }
        }

        Map<String, Integer> newMap = MapUtil.sortByValue(files);

        System.out.println("Size " + files.size());

        FileWriter fileWriter = new FileWriter("Result2.txt");

        for (String key : newMap.keySet()) {
            fileWriter.write("key/value: " + key + "/" + newMap.get(key) + "\n");
        }

        fileWriter.close();
    }
}
