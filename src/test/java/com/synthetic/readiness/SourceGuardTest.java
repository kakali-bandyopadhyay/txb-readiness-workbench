package com.synthetic.readiness;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class SourceGuardTest {
    @Test void sourceTemplatesDocsAndTestsStayFirmNeutralAndWordFree() throws IOException {
        String swift="SWI"+"FT";
        List<String> blocked=List.of("Gold"+"man","JP"+"Morgan","C"+"iti","HS"+"BC","Bar"+"clays","Deli"+"otte","Pw"+"C","Black"+"Rock",
            swift+" g"+"pi",swift+" G"+"o",swift+"R"+"ef",swift+"N"+"et","inter"+"view");
        Path root=Path.of(".").toAbsolutePath().normalize();
        List<Path> textFiles;
        try(Stream<Path> paths=Files.walk(root)) {
            textFiles=paths.filter(Files::isRegularFile)
                .filter(p->{String n=root.relativize(p).toString().replace('\\','/');return n.startsWith("src/")||n.startsWith("docs/")||n.equals("README.md")||n.equals("FINDINGS.md")||n.equals("pom.xml")||n.equals("Dockerfile")||n.equals("docker-compose.yml")||n.startsWith(".github/");})
                .filter(p->Set.of(".java",".html",".css",".sql",".yml",".md",".xml").contains(extension(p))||p.getFileName().toString().equals("Dockerfile"))
                .toList();
        }
        for(Path file:textFiles) {
            String contents=Files.readString(file);
            for(String term:blocked) assertFalse(Pattern.compile("(?i)\\b"+Pattern.quote(term)+"\\b").matcher(contents).find(),()->"Blocked term found in "+file);
        }
    }
    private static String extension(Path p) { String n=p.getFileName().toString(); int dot=n.lastIndexOf('.'); return dot<0?"":n.substring(dot).toLowerCase(Locale.ROOT); }
}

