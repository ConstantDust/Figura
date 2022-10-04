package org.moon.figura.parsers;

import net.minecraft.nbt.ByteArrayTag;
import org.moon.figura.FiguraMod;
import org.moon.figura.config.Config;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaScriptParser {

    Boolean error = false;

    // regex minify constants

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern string = Pattern.compile("\"(\\\\|\\\"|[^\"\n\r])*?\"|'(\\\\|\\'|[^'\n\r])*?'".stripIndent(), Pattern.MULTILINE);
    private static final Pattern multilineString = Pattern.compile("\\[(?<s>=*)\\[.*?](\\k<s>)]", Pattern.MULTILINE | Pattern.DOTALL);
    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern comments = Pattern.compile("--[^\n]*$", Pattern.MULTILINE);
    private static final Pattern multilineComment = Pattern.compile("--\\[(?<s>=*)\\[.*?](\\k<s>)]", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern newlines = Pattern.compile("^[\t ]*((\n|\n\r|\r\n)[\t ]*)?");
    private static final Pattern words = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern trailingNewlines = Pattern.compile("\n*$");
    private static final Pattern sheBangs = Pattern.compile("^#![^\n]*");

    // aggressive minify constants

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern allStrings = Pattern.compile(string.pattern() + "|" + multilineString.pattern());
    private static final Pattern whitespacePlus = Pattern.compile(" (\n+)?");

    public ByteArrayTag parseScript(String script) {
        if(FiguraMod.DEBUG_MODE) {
            String regexMinify = regexMinify(script);
            String agressiveMinify = aggressiveMinify(script);
            FiguraMod.LOGGER.info("Minified {} to {}, and {}", script.length(), regexMinify.length(), agressiveMinify.length());
        }
        error = true;
        var out = new ByteArrayTag((switch (Config.FORMAT_SCRIPT.asInt()){
            case 0 -> noMinifier(script);
            case 1 -> regexMinify(script);
            case 2 -> aggressiveMinify(script);
            default -> throw new IllegalStateException("Format_SCRIPT should not be %d, expecting 0 to %d".formatted(Config.FORMAT_SCRIPT.asInt(), Config.FORMAT_SCRIPT.enumList.size() - 1));
        }).getBytes(StandardCharsets.UTF_8));
        if (error) {
            FiguraMod.LOGGER.error("Failed to minify the script, likely to be syntax error");
        }
        return out;
    }

    String noMinifier(String script) {
        error = false;
        return script;
    }

    String regexMinify(String script) {

        StringBuilder builder = new StringBuilder(script);
        for (int i = 0; i < builder.length(); i++) {
            switch (builder.charAt(i)) {
                case '#' -> {
                    if (i > 0)
                        continue;
                    Matcher matcher = sheBangs.matcher(builder);
                    if(matcher.find()){
                        builder.delete(0, matcher.end());
                    }
                }
                case '\'', '"' -> {
                    Matcher matcher = string.matcher(builder);
                    if (!matcher.find(i) || !(matcher.start() == i)) return script;
                    i = matcher.end() - 1;
                }

                case '[' -> {
                    Matcher matcher = multilineString.matcher(builder);
                    if (matcher.find(i) && matcher.start() == i)
                        i = matcher.end() - 1;
                }

                case '-' -> {
                    if (i == builder.length() - 1) return script;
                    Matcher multiline = multilineComment.matcher(builder);
                    if (multiline.find(i) && multiline.start() == i) {
                        var breaks = builder.substring(i, multiline.end()).split("\n").length - 1;
                        builder.delete(i, multiline.end()).insert(i, "\n".repeat(breaks));
                        i--;
                        continue;
                    }
                    Matcher comment = comments.matcher(builder);
                    if (comment.find(i) && comment.start() == i) {
                        builder.delete(comment.start(), comment.end());
                        i--;
                    }
                }

                case ' ', '\t', '\n', '\r' -> {
                    Matcher newline = newlines.matcher(builder.substring(i));
                    if (newline.find())
                        if (newline.start() == 0)
                            builder.delete(i, i + newline.end()).insert(i, Optional.ofNullable(newline.group(1)).map(t -> "\n").orElse(" "));
                        else
                            FiguraMod.LOGGER.warn("script TODO appears to have invalid new line configuration");
                }

                default -> {
                    Matcher word = words.matcher(builder);
                    if (word.find(i) && word.start() == i)
                        i = word.end() - 1;
                }
            }
        }
        Matcher trailingNewline = trailingNewlines.matcher(builder);
        if(trailingNewline.find()){
            builder.replace(trailingNewline.start(), trailingNewline.end(), "\n");
        }
        error = false;
        return builder.toString();
    }


    String aggressiveMinify(String script){
        String start = regexMinify(script);
        StringBuilder builder = new StringBuilder(start);

        for (int i = 0; i < builder.length(); i++) {
            switch (builder.charAt(i)){
                case '\'', '"', '[' -> {
                    Matcher matcher = allStrings.matcher(builder);
                    if (matcher.find(i) && matcher.start() == i)
                        i = matcher.end() - 1;

                }
                case ' ', '\n' -> {
                    if(builder.charAt(i) == '\n')
                        builder.insert(i, ' ');
                    Matcher matcher = whitespacePlus.matcher(builder);
                    if(matcher.find(i) && matcher.start() == i) {
                        var al = matcher.start() > 0 && matcher.end() < builder.length() - 1 &&
                                words.matcher("a" + builder.substring(matcher.start() - 1, matcher.start())).matches() &&
                                words.matcher(builder.substring(matcher.end(), matcher.end() + 1)).matches();
//                        String group = matcher.group(1);
//                        builder.delete(i, matcher.end()).insert(i, Optional.ofNullable(group).map(l -> "\n").orElse(al ? " " : ""));
                        builder.delete(i, matcher.end()).insert(i, al ? " " : "");
                    }
                }

                default -> {
                    Matcher word = words.matcher(builder);
                    if (word.find(i) && word.start() == i)
                        i = word.end() - 1;
                }
            }
        }
        return builder.toString();
    }

    enum TokenType {

    }
}