package it.shaunyl.enver.database;

import java.util.*;

/**
 * 
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
@Deprecated
public class MultibyteCharacters {

    public static List<Character> list2 = Arrays.asList(
            // accented characters
            'è', 'é', 'È', 'É', 'Ë', 'Ê', 'ë', 'ê',
            'ò', 'ô', 'ö', 'õ', 'Ó', 'Ò', 'Ô', 'Õ', 'Ö', 'ó', 'ð',
            'ù', 'û', 'ü', 'ú', 'Ù', 'Ú', 'Û', 'Ü',
            'à', 'À', 'Á', 'Â', 'Ã', 'Ä', 'Å', 'ä', 'ã', 'â', 'á',
            'ì', 'ï', 'î', 'í', 'Ï', 'Î', 'Í', 'Ì',
            // danish characters
            'å',
            'æ',
            'ø',
            'Å',
            'Ø',
            'Æ',
            // other characters
            '@', 'ß', 'ñ', 'ç', '©', '®', '°', '§', 'Ñ', 'Ý', 'Ç', 'µ', '£', 'ý', '÷'
            , 'þ', 'ÿ', '·', 'Ð', 'Þ');
    public static List<Character> list3 = Arrays.asList(
            '€');
}
