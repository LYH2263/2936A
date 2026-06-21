package com.exam.config;

import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    @Transactional
    public CommandLineRunner initData(UserRepository userRepository,
                                      ExamRepository examRepository,
                                      QuestionRepository questionRepository,
                                      ExamQuestionRepository examQuestionRepository,
                                      SubmissionRepository submissionRepository,
                                      SubmissionAnswerRepository submissionAnswerRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = null;
            User teacher1 = null;
            User teacher2 = null;

            if (userRepository.count() == 0) {
                admin = createUser(userRepository, passwordEncoder, "admin", "ADMIN", "管理员");
                teacher1 = createUser(userRepository, passwordEncoder, "1001", "TEACHER", "张老师");
                teacher2 = createUser(userRepository, passwordEncoder, "1002", "TEACHER", "王老师");
                createUser(userRepository, passwordEncoder, "2024001", "STUDENT", "李同学");
                createUser(userRepository, passwordEncoder, "2024002", "STUDENT", "陈同学");
            } else {
                admin = userRepository.findByUsername("admin").orElse(null);
                teacher1 = userRepository.findByUsername("1001").orElseGet(() ->
                    createUser(userRepository, passwordEncoder, "1001", "TEACHER", "张老师"));
                if (userRepository.findByUsername("2024001").isEmpty()) {
                    createUser(userRepository, passwordEncoder, "2024001", "STUDENT", "李同学");
                }
                userRepository.findAll().forEach(u -> {
                    if (u.getCreatedAt() == null) {
                        u.setCreatedAt(java.time.LocalDateTime.now());
                        userRepository.save(u);
                    }
                });
                teacher2 = userRepository.findByUsername("1002").orElseGet(() ->
                    createUser(userRepository, passwordEncoder, "1002", "TEACHER", "王老师"));
            }

            long currentQuestionCount = questionRepository.count();
            boolean shouldSeedQuestions = currentQuestionCount == 0;
            boolean shouldPatchQuestions = currentQuestionCount > 0 && needsPatching(questionRepository);

            if (shouldPatchQuestions) {
                patchExistingQuestions(questionRepository);
                System.out.println("Existing questions patched with subject/difficulty fields.");
            }

            if (shouldSeedQuestions && teacher1 != null) {
                createMathExam(examRepository, questionRepository, examQuestionRepository, teacher1);
                createScienceExam(examRepository, questionRepository, examQuestionRepository, teacher1);
                if (teacher2 != null) {
                    createHistoryExam(examRepository, questionRepository, examQuestionRepository, teacher2);
                    createTechExam(examRepository, questionRepository, examQuestionRepository, teacher2);
                }
                seedJavaQuestionBank(questionRepository, teacher1);
                System.out.println("Data Seeding Completed with Realistic Chinese Data!");
            } else if (!shouldSeedQuestions && currentQuestionCount < 15) {
                if (teacher1 != null) {
                    seedJavaQuestionBank(questionRepository, teacher1);
                    System.out.println("Java question bank seeded.");
                }
            }
        };
    }

    private boolean needsPatching(QuestionRepository questionRepository) {
        List<Question> sample = questionRepository.findAll();
        if (sample.isEmpty()) return false;
        for (Question q : sample) {
            if (q.getSubject() == null || q.getDifficulty() == null) {
                return true;
            }
        }
        return false;
    }

    private void patchExistingQuestions(QuestionRepository questionRepository) {
        List<Question> all = questionRepository.findAll();
        for (Question q : all) {
            boolean changed = false;
            if (q.getSubject() == null || q.getSubject().isEmpty()) {
                String content = q.getContent() == null ? "" : q.getContent();
                if (content.contains("导数") || content.contains("函数") || content.contains("矩阵") || content.contains("微积分") || content.contains("三角")) {
                    q.setSubject("数学");
                    q.setKnowledgePoint(content.contains("三角") ? "三角函数" : (content.contains("矩阵") ? "线性代数" : "微积分"));
                    q.setDifficulty(3);
                } else if (content.contains("化学") || content.contains("原子") || content.contains("分子") || content.contains("HO") || content.contains("木星") || content.contains("行星") || content.contains("细胞") || content.contains("生物")) {
                    q.setSubject("自然科学");
                    q.setKnowledgePoint(content.contains("木星") || content.contains("行星") ? "天文学" : (content.contains("HO") ? "化学基础" : "生物基础"));
                    q.setDifficulty(content.contains("木星") ? 2 : 1);
                } else if (content.contains("秦始皇") || content.contains("唐朝") || content.contains("历史") || content.contains("皇帝") || content.contains("开国")) {
                    q.setSubject("历史");
                    q.setKnowledgePoint("中国古代史");
                    q.setDifficulty(2);
                } else if (content.contains("Windows") || content.contains("Linux") || content.contains("电路") || content.contains("操作系统") || content.contains("计算机")) {
                    q.setSubject("计算机基础");
                    q.setKnowledgePoint(content.contains("操作系统") || content.contains("Windows") || content.contains("Linux") ? "操作系统" : "计算机组成原理");
                    q.setDifficulty(2);
                } else {
                    q.setSubject("综合");
                    q.setDifficulty(3);
                }
                changed = true;
            }
            if (q.getDifficulty() == null) {
                q.setDifficulty(3);
                changed = true;
            }
            if (changed) {
                questionRepository.save(q);
            }
        }
    }

    private User createUser(UserRepository repo, PasswordEncoder encoder, String username, String role, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode("123456"));
        user.setRole(role);
        user.setFullName(fullName);
        return repo.save(user);
    }

    private void createMathExam(ExamRepository examRepo, QuestionRepository questionRepo, ExamQuestionRepository eqRepo, User creator) {
        Exam exam = new Exam();
        exam.setTitle("高等数学期中考试");
        exam.setCourse("数学");
        exam.setDescription("本试卷涵盖微积分、线性代数基础知识。请在规定时间内完成。");
        exam.setDuration(90);
        exam.setState("PUBLISHED");
        exam.setCreator(creator);
        exam.setStartTime(LocalDateTime.now().minusDays(1));
        exam.setEndTime(LocalDateTime.now().plusDays(7));
        exam.setCoverUrl("/covers/math.png");
        exam = examRepo.save(exam);

        List<Question> questions = new ArrayList<>();
        questions.add(createFullQuestion(questionRepo, creator, "SINGLE", "数学", "微积分", 3,
            "函数 f(x) = x^2 在 x=1 处的导数是？",
            "[{\"label\":\"A\",\"text\":\"1\"},{\"label\":\"B\",\"text\":\"2\"},{\"label\":\"C\",\"text\":\"3\"},{\"label\":\"D\",\"text\":\"0\"}]", "B", "f'(x)=2x, f'(1)=2", 5));
        questions.add(createFullQuestion(questionRepo, creator, "JUDGE", "数学", "线性代数", 2,
            "矩阵乘法满足交换律。", null, "FALSE", "矩阵乘法一般不满足交换律 AB != BA", 5));
        questions.add(createFullQuestion(questionRepo, creator, "MULTI", "数学", "三角函数", 3,
            "下列哪些是三角函数？",
            "[{\"label\":\"A\",\"text\":\"sin(x)\"},{\"label\":\"B\",\"text\":\"cos(x)\"},{\"label\":\"C\",\"text\":\"log(x)\"},{\"label\":\"D\",\"text\":\"tan(x)\"}]", "ABD", null, 5));

        linkQuestionsToExam(eqRepo, exam, questions);
    }

    private void createScienceExam(ExamRepository examRepo, QuestionRepository questionRepo, ExamQuestionRepository eqRepo, User creator) {
        Exam exam = new Exam();
        exam.setTitle("自然科学综合测试");
        exam.setCourse("自然科学");
        exam.setDescription("测试物理、化学、生物基础概念。");
        exam.setDuration(60);
        exam.setState("PUBLISHED");
        exam.setCreator(creator);
        exam.setStartTime(LocalDateTime.now());
        exam.setEndTime(LocalDateTime.now().plusDays(30));
        exam.setCoverUrl("/covers/science.png");
        exam = examRepo.save(exam);

        List<Question> questions = new ArrayList<>();
        questions.add(createFullQuestion(questionRepo, creator, "SINGLE", "自然科学", "化学基础", 1,
            "水分子的化学式是？",
            "[{\"label\":\"A\",\"text\":\"HO\"},{\"label\":\"B\",\"text\":\"H2O\"},{\"label\":\"C\",\"text\":\"H2O2\"},{\"label\":\"D\",\"text\":\"OH\"}]", "B", "两个氢原子一个氧原子", 5));
        questions.add(createFullQuestion(questionRepo, creator, "SINGLE", "自然科学", "天文学", 2,
            "太阳系中最大的行星是？",
            "[{\"label\":\"A\",\"text\":\"地球\"},{\"label\":\"B\",\"text\":\"火星\"},{\"label\":\"C\",\"text\":\"木星\"},{\"label\":\"D\",\"text\":\"土星\"}]", "C", "木星体积最大", 5));

        linkQuestionsToExam(eqRepo, exam, questions);
    }

    private void createHistoryExam(ExamRepository examRepo, QuestionRepository questionRepo, ExamQuestionRepository eqRepo, User creator) {
        Exam exam = new Exam();
        exam.setTitle("中国古代史");
        exam.setCourse("历史");
        exam.setDescription("考察秦汉至明清的历史变迁。");
        exam.setDuration(45);
        exam.setState("PUBLISHED");
        exam.setCreator(creator);
        exam.setStartTime(LocalDateTime.now().minusDays(5));
        exam.setEndTime(LocalDateTime.now().plusDays(5));
        exam.setCoverUrl("/covers/history.png");
        exam = examRepo.save(exam);

        List<Question> questions = new ArrayList<>();
        questions.add(createFullQuestion(questionRepo, creator, "SINGLE", "历史", "中国古代史", 2,
            "秦始皇统一六国的时间是？",
            "[{\"label\":\"A\",\"text\":\"公元前221年\"},{\"label\":\"B\",\"text\":\"公元221年\"},{\"label\":\"C\",\"text\":\"公元前202年\"},{\"label\":\"D\",\"text\":\"公元202年\"}]", "A", null, 10));
        questions.add(createFullQuestion(questionRepo, creator, "JUDGE", "历史", "中国古代史", 1,
            "唐朝的开国皇帝是李世民。", null, "FALSE", "是李渊", 5));

        linkQuestionsToExam(eqRepo, exam, questions);
    }

    private void createTechExam(ExamRepository examRepo, QuestionRepository questionRepo, ExamQuestionRepository eqRepo, User creator) {
        Exam exam = new Exam();
        exam.setTitle("计算机基础知识");
        exam.setCourse("计算机基础");
        exam.setDescription("计算机组成原理、网络、操作系统基础。");
        exam.setDuration(120);
        exam.setState("PUBLISHED");
        exam.setCreator(creator);
        exam = examRepo.save(exam);
        exam.setCoverUrl("/covers/tech.png");
        exam = examRepo.save(exam);

        List<Question> questions = new ArrayList<>();
        questions.add(createFullQuestion(questionRepo, creator, "SINGLE", "计算机基础", "计算机组成原理", 1,
            "下图所示的电路元件符号代表什么？<br><img src='/images/circuit.png' style='max-width:300px;' />",
            "[{\"label\":\"A\",\"text\":\"电阻\"},{\"label\":\"B\",\"text\":\"电容\"},{\"label\":\"C\",\"text\":\"灯泡\"},{\"label\":\"D\",\"text\":\"开关\"}]", "D", null, 5));
        questions.add(createFullQuestion(questionRepo, creator, "MULTI", "计算机基础", "操作系统", 2,
            "下列属于操作系统的是？",
            "[{\"label\":\"A\",\"text\":\"Windows\"},{\"label\":\"B\",\"text\":\"Linux\"},{\"label\":\"C\",\"text\":\"Office\"},{\"label\":\"D\",\"text\":\"macOS\"}]", "ABD", null, 5));

        linkQuestionsToExam(eqRepo, exam, questions);
    }

    private void seedJavaQuestionBank(QuestionRepository questionRepo, User creator) {
        List<Question> bank = new ArrayList<>();

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "Java基础语法", 1,
            "Java 程序的入口方法签名是？",
            "[{\"label\":\"A\",\"text\":\"void start()\"},{\"label\":\"B\",\"text\":\"public static void main(String[] args)\"},{\"label\":\"C\",\"text\":\"public void main(String args)\"},{\"label\":\"D\",\"text\":\"static void main()\"}]",
            "B", "Java 程序必须以 public static void main(String[] args) 作为入口。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "面向对象", 2,
            "以下哪个关键字用于实现继承？",
            "[{\"label\":\"A\",\"text\":\"implements\"},{\"label\":\"B\",\"text\":\"extends\"},{\"label\":\"C\",\"text\":\"interface\"},{\"label\":\"D\",\"text\":\"abstract\"}]",
            "B", "extends 用于类继承类；implements 用于类实现接口。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "集合框架", 3,
            "以下哪个集合允许重复元素且保证插入顺序？",
            "[{\"label\":\"A\",\"text\":\"HashSet\"},{\"label\":\"B\",\"text\":\"TreeSet\"},{\"label\":\"C\",\"text\":\"ArrayList\"},{\"label\":\"D\",\"text\":\"HashMap\"}]",
            "C", "ArrayList 是 List 实现，允许重复并保持插入顺序；Set 系列不允许重复。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "JUDGE", "Java", "Java基础语法", 1,
            "Java 中 int 类型占 4 个字节。",
            null, "TRUE", "Java 基本类型：byte(1), short(2), int(4), long(8)。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "JUDGE", "Java", "面向对象", 2,
            "Java 的类可以同时继承多个父类。",
            null, "FALSE", "Java 只支持单继承；多继承通过接口 (interface) 实现。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "JUDGE", "Java", "异常处理", 3,
            "Error 类型异常可以被 try-catch 捕获并恢复。",
            null, "FALSE", "Error 属于系统级严重错误（如 OutOfMemoryError），通常不建议也无法可靠恢复。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "MULTI", "Java", "面向对象", 3,
            "以下哪些是 Java 访问权限修饰符？",
            "[{\"label\":\"A\",\"text\":\"public\"},{\"label\":\"B\",\"text\":\"private\"},{\"label\":\"C\",\"text\":\"static\"},{\"label\":\"D\",\"text\":\"protected\"}]",
            "ABD", "四种访问级别：public / protected / default(包级) / private；static 不是访问修饰符。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "MULTI", "Java", "集合框架", 3,
            "以下哪些接口属于 Java 集合框架？",
            "[{\"label\":\"A\",\"text\":\"List\"},{\"label\":\"B\",\"text\":\"Map\"},{\"label\":\"C\",\"text\":\"Set\"},{\"label\":\"D\",\"text\":\"Array\"}]",
            "ABC", "Collection 体系含 List、Set；Map 为独立体系；Array 是原生数组而非接口。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "多线程", 4,
            "以下哪种方式最安全地实现单例模式（延迟加载且线程安全）？",
            "[{\"label\":\"A\",\"text\":\"直接在静态字段初始化\"},{\"label\":\"B\",\"text\":\"双重检查锁定 (DCL) + volatile\"},{\"label\":\"C\",\"text\":\"synchronized 修饰 getInstance 方法\"},{\"label\":\"D\",\"text\":\"静态内部类 (Initialization-on-demand holder)\"}]",
            "D", "静态内部类方式由 JVM 类加载机制保证线程安全与延迟加载，是《Effective Java》推荐实现。", 10));

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "JVM内存模型", 4,
            "以下哪个内存区域不属于线程私有？",
            "[{\"label\":\"A\",\"text\":\"程序计数器\"},{\"label\":\"B\",\"text\":\"虚拟机栈\"},{\"label\":\"C\",\"text\":\"方法区 / 元空间\"},{\"label\":\"D\",\"text\":\"本地方法栈\"}]",
            "C", "方法区/元空间、堆 是线程共享区域；计数器、栈是线程私有。", 10));

        bank.add(createFullQuestion(questionRepo, creator, "SINGLE", "Java", "Java基础语法", 2,
            "String s = new String(\"abc\"); 会创建几个对象？",
            "[{\"label\":\"A\",\"text\":\"1 个\"},{\"label\":\"B\",\"text\":\"2 个\"},{\"label\":\"C\",\"text\":\"1 或 2 个\"},{\"label\":\"D\",\"text\":\"0 个\"}]",
            "C", "若字符串常量池尚无 \"abc\"，会在池中创建 1 个 + 堆中 1 个 new 对象；若池中已有则只创建堆中 1 个。", 5));

        bank.add(createFullQuestion(questionRepo, creator, "SHORT", "Java", "面向对象", 3,
            "请简述 Java 中重载 (Overload) 和重写 (Override) 的区别。",
            null, "方法名 参数列表 返回值 访问权限 注解 子类 父类",
            "重载：同一个类中，方法名相同、参数列表不同（类型/个数/顺序），与返回值、访问修饰符无关。重写：父子类中，方法签名完全相同，访问权限不能更严格，返回值协变，需 @Override 注解。",
            10));
    }

    private Question createFullQuestion(QuestionRepository repo, User creator, String type,
                                         String subject, String knowledgePoint, Integer difficulty,
                                         String content, String options, String answer, String analysis, int score) {
        Question q = new Question();
        q.setCreator(creator);
        q.setType(type);
        q.setSubject(subject);
        q.setKnowledgePoint(knowledgePoint);
        q.setDifficulty(difficulty);
        q.setContent(content);
        q.setOptions(options);
        q.setAnswer(answer);
        q.setAnalysis(analysis);
        q.setDefaultScore(score);
        return repo.save(q);
    }

    private Question createQuestion(QuestionRepository repo, User creator, String type, String content, String options, String answer, String analysis, int score) {
        Question q = new Question();
        q.setCreator(creator);
        q.setType(type);
        q.setContent(content);
        q.setOptions(options);
        q.setAnswer(answer);
        q.setAnalysis(analysis);
        q.setDefaultScore(score);
        return repo.save(q);
    }

    private void linkQuestionsToExam(ExamQuestionRepository eqRepo, Exam exam, List<Question> questions) {
        for (int i = 0; i < questions.size(); i++) {
            ExamQuestion eq = new ExamQuestion();
            eq.setExam(exam);
            eq.setQuestion(questions.get(i));
            eq.setScore(questions.get(i).getDefaultScore());
            eq.setSequence(i + 1);
            eqRepo.save(eq);
        }
    }
}
