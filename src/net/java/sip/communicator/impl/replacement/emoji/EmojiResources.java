// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.replacement.emoji;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.sip.communicator.service.replacement.InsertableIcon;
import net.java.sip.communicator.service.replacement.TabOfInsertableIcons;
import net.java.sip.communicator.util.Logger;

import javax.swing.ImageIcon;

import com.google.common.annotations.VisibleForTesting;

/**
 * The resources used to access the emoji icons
 */
public class EmojiResources
{
    /**
     * The Logger used by the EmoticonReplacementService
     * class and its instances for logging output.
     */
    private static final Logger sLog
        = Logger.getLogger(EmojiResources.class);

    /**
     * The directory in which all the emoji images are stored
     */
    private static final String EMOJI_IMAGE_DIR = EmojiActivator.getResources().getSettingsString("net.java.sip.communicator.impl.replacement.emoji.EMOJI_DIR");

    /**
     * Prefix for URLs of emojis picture files in the emoji directory.
     */
    private static final String EMOJI_IMAGE_URL_PREFIX = initEmojiImageUrlPrefix();

   /**
    * The list of tabs to be displayed in the emoji selector box.
    */
    private static List<TabOfInsertableIcons> listOfTabs = new ArrayList<>();

    /**
    * Set to true once all emoji images have been loaded
    */
    private static boolean mAllEmojisLoaded = false;

    /**
     * Generate a prefix to which Emoji unicode hex codepoints can be appended to make a valid
     * OSGi BundleURL for the emoji glyph image resource.  This approach avoids a double-slash
     * before "emoji_u", because that can cause emoji images to fail to load.
     * @return String URL base ending in "emoji_u" for appending codepoint hex sequences, e.g.
     * + "1f607.png" to make "bundle://<GUID>.0:1/<PATH>/emoji_u1f607.png" - or multiple codepoints
     * e.g. "emoji_u0034_20e3.png" - as returned by getFilepathSuffixFromCodepoints method.
     */
    private static String initEmojiImageUrlPrefix() {
        try {
            final URL baseUrl = EmojiActivator.getResources().getImageURLForPath(EMOJI_IMAGE_DIR);
            return baseUrl.toURI().resolve("./emoji_u").toString();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Produces an InsertableIcon from a codepoint.
    * @param codepoint
    * @return InsertableIcon
    */
    private static InsertableIcon getInsertableIconFromCodepoint(int codepoint)
    {
        String filepath = getFilepathFromCodepoint(codepoint);

        return new InsertableIcon(String.valueOf(Character.toChars(codepoint)),
                                  filepath,
                                  EmojiActivator.getResources().getImageFromPath(filepath));
    }

    /**
     * Given a list of emoji codepoints and a (possibly empty) iconList, creates
     * InsertableIcons from the codepoints and adds these to the iconList.
     * @param iconList
     */
    private static void addIconsToIconList(List<Integer> emojiCodepoints , List<InsertableIcon> iconList)
    {
        for (Integer codepoint : emojiCodepoints)
        {
            iconList.add(getInsertableIconFromCodepoint(codepoint));
        }
    }

    /**
     * Creates the tab object for all the smileys and people-related emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getSmileysAndPeopleTab()
    {
        // Creates the set of codepoints corresponding to smileys and people emojis
        // Note that the reason this has to be hardcoded in is that the unicode
        // codepoint order does not at all group the emojis into sensible
        // categories e.g. consecutive codepoints sometimes represent a face and
        // then an animal
        // Note that there's no 128405 (the middle finger emoji) in this list, as it's not an appropriate
        // emoji to appear in the selector box. We keep the resource file though, so the unicode can still
        // be displayed when it's received.
        List<Integer> smileysAndPeopleCodepoints = new ArrayList<>(Arrays.asList(128512, 128515, 128516,
            128513, 128518, 128517, 128514, 129315, 9786, 128522, 128519, 128578, 128579, 128521, 128524, 128525,
            128536, 128535, 128537, 128538, 128523, 128539, 128541, 128540, 129322, 129320, 129488, 129299,
            128526, 129321, 128527, 128530, 128542, 128532, 128543, 128533, 128577, 9785, 128547, 128534, 128555,
            128553, 128546, 128557, 128548, 128544, 128545, 129324, 129327, 128563, 128561, 128552, 128560,
            128549, 128531, 129303, 129300, 129325, 129323, 129317, 128566, 128528, 128529, 128556, 128580, 128559,
            128550, 128551, 128558, 128562, 128564, 129316, 128554, 128565, 129296, 129314, 129326, 129319,
            128567, 129298, 129301, 129297, 129312, 128520, 128127, 128121, 128122, 129313, 128169, 128123, 128128,
            9760, 128125, 128126, 129302, 127875, 128570, 128568, 128569, 128571, 128572, 128573, 128576, 128575,
            128574, 129330, 128080, 128079, 129309, 128077, 128078, 128074, 9994, 129307, 129308, 129310, 129311,
            129304, 128076, 128072, 128073, 128070, 128071, 9757, 9995, 129306, 128400, 128406, 128075, 129305,
            128170, 9997, 128591, 128141, 128132, 128139, 128068, 128069, 128066, 128067, 128099, 128065,
            128064, 129504, 128483, 128100, 128101, 128118, 128103, 129490, 128102, 128105, 129492, 128117, 129491,
            128116, 129331, 128131, 128378, 128372, 129509, 128090, 128085, 128086, 128084, 128087, 128089, 128088,
            128096, 128097, 128098, 128094, 128095, 129510, 129508, 129507, 127913, 129506, 128082, 127891, 9937,
            128081, 128093, 128091, 128092, 128188, 127890, 128083, 128374, 127746));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(smileysAndPeopleCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_smiley.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

   /**
    * Creates the tab object for all the animals and nature emojis.
    * @return tab of emojis
    */
    private static TabOfInsertableIcons getAnimalsAndNatureTab()
    {
        // Creates the set of codepoints corresponding to animals and nature emojis
        List<Integer> animalsAndNatureCodepoints = new ArrayList<>(Arrays.asList(128054, 128049, 128045, 128057,
            128048, 129418, 128059, 128060, 128040, 128047, 129409, 128046, 128055, 128061, 128056, 128053, 128584,
            128585, 128586, 128018, 128020, 128039, 128038, 128036, 128035, 128037, 129414, 129413, 129417, 129415,
            128058, 128023, 128052, 129412, 128029, 128027, 129419, 128012, 128026, 128030, 128028, 129431, 128375,
            128376, 129410, 128034, 128013, 129422, 129430, 129429, 128025, 129425, 129424, 129408, 128033, 128032,
            128031, 128044, 128051, 128011, 129416, 128010, 128005, 128006, 129427, 129421, 128024, 129423, 128042,
            128043, 129426, 128003, 128002, 128004, 128014, 128022, 128015, 128017, 128016, 129420, 128021, 128041,
            128008, 128019, 129411, 128330, 128007, 128001, 128000, 128063, 129428, 128062, 128009, 128050, 127797,
            127876, 127794, 127795, 127796, 127793, 127807, 9752, 127808, 127885, 127883, 127811, 127810, 127809,
            127812, 127806, 128144, 127799, 127801, 129344, 127802, 127800, 127804, 127803, 127774, 127773, 127771,
            127772, 127770, 127765, 127766, 127767, 127768, 127761, 127762, 127763, 127764, 127769, 127758, 127757,
            127759, 128171, 11088, 127775, 10024, 9889, 9732, 128165, 128293, 127786, 127752, 9728, 127780, 9925,
            127781, 9729, 127782, 127783, 9928, 127785, 127784, 10052, 9731, 9924, 127788, 128168, 128167, 128166,
            9748, 9730, 127754, 127787));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(animalsAndNatureCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_animal.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * Creates the tab object for all the food and drink emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getFoodAndDrinkTab()
    {
        // Creates the set of codepoints corresponding to food and drink emojis
        List<Integer> foodAndDrinkCodepoints = new ArrayList<>(Arrays.asList(127823, 127822, 127824, 127818, 127819,
            127820, 127817, 127815, 127827, 127816, 127826, 127825, 127821, 129381, 129373, 127813, 127814, 129361,
            129382, 129362, 127798, 127805, 129365, 129364, 127840, 129360, 127838, 129366, 129384, 129472, 129370,
            127859, 129374, 129363, 129385, 127831, 127830, 127789, 127828, 127839, 127829, 129386, 129369, 127790,
            127791, 129367, 129368, 129387, 127837, 127836, 127858, 127835, 127843, 127857, 129375, 127844, 127833,
            127834, 127832, 127845, 129376, 127842, 127841, 127847, 127848, 127846, 129383, 127856, 127874, 127854,
            127853, 127852, 127851, 127871, 127849, 127850, 127792, 129372, 127855, 129371, 127868, 9749, 127861,
            129380, 127862, 127866, 127867, 129346, 127863, 129347, 127864, 127865, 127870, 129348, 127860, 127869,
            129379, 129377, 129378));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(foodAndDrinkCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_food.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * Creates the tab object for all the activities emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getActivitiesTab()
    {
        // Creates the set of codepoints corresponding to activities emojis
        List<Integer> activitiesCodepoints = new ArrayList<>(Arrays.asList(9917, 127936, 127944, 9918, 127934, 127952, 127945, 127921,
            127955, 127992, 129349, 127954, 127953, 127951, 9971, 127993, 127907, 129354, 129355, 127933, 9976, 129356,
            128759, 127935, 9975, 127938, 129338, 127942, 129351, 129352, 129353, 127941, 127894, 127989, 127895, 127915,
            127903, 127914, 127917, 127912, 127916, 127908, 127911, 127932, 127929, 129345, 127927, 127930, 127928, 127931,
            127922, 127919, 127923, 127918, 127920));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(activitiesCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_sport.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * Creates the tab object for all the travel and places emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getTravelAndPlacesTab()
    {
        // Creates the set of codepoints corresponding to travel and places emojis
        List<Integer> travelCodepoints = new ArrayList<>(Arrays.asList(128663, 128661, 128665, 128652, 128654, 127950, 128659,
            128657, 128658, 128656, 128666, 128667, 128668, 128756, 128690, 128757, 127949, 128680, 128660, 128653, 128664, 128662,
            128673, 128672, 128671, 128643, 128651, 128670, 128669, 128644, 128645, 128648, 128642, 128646, 128647, 128650, 128649,
            9992, 128747, 128748, 128745, 128186, 128752, 128640, 128760, 128641, 128758, 9973, 128676, 128741, 128755, 9972, 128674,
            9875, 9981, 128679, 128678, 128677, 128655, 128506, 128511, 128509, 128508, 127984, 127983, 127967, 127905, 127906,
            127904, 9970, 9969, 127958, 127965, 127964, 127755, 9968, 127956, 128507, 127957, 9978, 127968, 127969, 127960, 127962,
            127959, 127981, 127970, 127980, 127971, 127972, 127973, 127974, 127976, 127978, 127979, 127977, 128146, 127963, 9962,
            128332, 128333, 128331, 9961, 128740, 128739, 128510, 127889, 127966, 127749, 127748, 127776, 127879, 127878, 127751,
            127750, 127961, 127747, 127756, 127753, 127745));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(travelCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_car.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * Creates the tab object for all the objects emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getObjectsTab()
    {
        // Creates the set of codepoints corresponding to object emojis
        List<Integer> objectsCodepoints = new ArrayList<>(Arrays.asList(8986, 128241, 128242, 128187, 9000, 128421, 128424,
            128433, 128434, 128377, 128476, 128189, 128190, 128191, 128192, 128252, 128247, 128248, 128249, 127909, 128253,
            127902, 128222, 9742, 128223, 128224, 128250, 128251, 127897, 127898, 127899, 9201, 9202, 9200, 128368, 8987, 9203,
            128225, 128267, 128268, 128161, 128294, 128367, 128465, 128738, 128184, 128181, 128180, 128182, 128183, 128176,
            128179, 128142, 9878, 128295, 128296, 9874, 128736, 9935, 128297, 9881, 9939, 128299, 128163, 128298, 128481, 9876,
            128737, 128684, 9904, 9905, 127994, 128302, 128255, 128136, 9879, 128301, 128300, 128371, 128138, 128137, 127777,
            128701, 128688, 128703, 128705, 128704, 128718, 128273, 128477, 128682, 128715, 128719, 128716, 128444, 128717,
            128722, 127873, 127880, 127887, 127872, 127882, 127881, 127886, 127982, 127888, 9993, 128233, 128232, 128231,
            128140, 128229, 128228, 128230, 127991, 128234, 128235, 128236, 128237, 128238, 128239, 128220, 128195, 128196,
            128209, 128202, 128200, 128201, 128466, 128467, 128198, 128197, 128199, 128451, 128499, 128452, 128203, 128193,
            128194, 128450, 128478, 128240, 128211, 128212, 128210, 128213, 128215, 128216, 128217, 128218, 128214, 128278,
            128279, 128206, 128391, 128208, 128207, 128204, 128205, 9986, 128394, 128395, 10002, 128396, 128397, 128221, 9999,
            128269, 128270, 128271, 128272, 128274, 128275));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(objectsCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_objects.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * Creates the tab object for all the symbols emojis.
     * @return tab of emojis
     */
    private static TabOfInsertableIcons getSymbolsTab()
    {
        // Creates the set of codepoints corresponding to symbol emojis
        List<Integer> symbolsCodepoints = new ArrayList<>(Arrays.asList(10084, 129505, 128155, 128154, 128153, 128156, 128420,
            128148, 10083, 128149, 128158, 128147, 128151, 128150, 128152, 128157, 128159, 9774, 10013, 9770, 128329, 9784, 10017,
            128303, 128334, 9775, 9766, 128720, 9934, 9800, 9801, 9802, 9803, 9804, 9805, 9806, 9807, 9808, 9809, 9810, 9811,
            127380, 9883, 127569, 9762, 9763, 128244, 128243, 127542, 127514, 127544, 127546, 127543, 10036, 127386, 128174, 127568,
            12953, 12951, 127540, 127541, 127545, 127538, 127344, 127345, 127374, 127377, 127358, 127384, 10060, 11093, 128721,
            9940, 128219, 128683, 128175, 128162, 9832, 128695, 128687, 128691, 128689, 128286, 128245, 128685, 10071, 10069, 10067,
            10068, 8252, 8265, 128261, 128262, 12349, 9888, 128696, 128305, 9884, 128304, 9851, 9989, 127535, 128185, 10055, 10035,
            10062, 127760, 128160, 9410, 127744, 128164, 127975, 128702, 9855, 127359, 127539, 127490, 128706, 128707, 128708,
            128709, 128697, 128698, 128700, 128699, 128686, 127910, 128246, 127489, 128291, 8505, 128292, 128289, 128288, 127382,
            127383, 127385, 127378, 127381, 127379, 8419, 128287, 128290,
            9167, 9654, 9208, 9199, 9209, 9210, 9197, 9198, 9193, 9194, 9195, 9196, 9664, 128316, 128317, 10145, 11013, 11014,
            11015, 8599, 8600, 8601, 8598, 8597, 8596, 8618, 8617, 10548, 10549, 128256, 128257, 128258, 128260, 128259, 127925,
            127926, 10133, 10134, 10135, 10006, 128178, 128177, 8482, 169, 174, 12336, 10160, 10175, 128282, 128281, 128283, 128285,
            128284, 10004, 9745, 128280, 9898, 9899, 128308, 128309, 128314, 128315, 128312, 128313, 128310, 128311, 128307, 128306,
            9642, 9643, 9726, 9725, 9724, 9723, 11035, 11036, 128264, 128263, 128265, 128266, 128276, 128277, 128227, 128226, 128065,
            128488, 128172, 128173, 128495, 9824, 9827, 9829, 9830, 127183, 127924, 126980, 128336, 128337, 128338, 128339, 128340,
            128341, 128342, 128343, 128344, 128345, 128346, 128347, 128348, 128349, 128350, 128351, 128352, 128353, 128354, 128355,
            128356, 128357, 128358, 128359));

        // Adds the emoji icons to the tab and sets the icon to display on the
        // tab
        List<InsertableIcon> iconList = new ArrayList<>();

        addIconsToIconList(symbolsCodepoints, iconList);

        ImageIcon tabIcon = EmojiActivator.getResources().getImageFromPath(EMOJI_IMAGE_DIR + "/tab_symbol.png").resolve();
        return new TabOfInsertableIcons(iconList, tabIcon);
    }

    /**
     * The set of codepoints of supported emojis
     */
    private static Set<Integer> setOfEmojiCodepointsNotStartingSequence;
    private static Set<List<Integer>> setOfEmojiCodepointSequences;

    private static final Pattern emojiFilenamePattern =
        Pattern.compile("emoji_u(([0-9a-z]{4,5})(_[0-9a-z]{4,5})?(_[0-9a-z]{4,5})?(_[0-9a-z]{4,5})?).png");

    /**
     * From the filepath of an emoji image - so something like
     * resources/images/.../emoji_u1f525.png, this returns just the unicode
     * part e.g. 1f525, in the example above
     * @param filename
     * @return codepoint of the image, null if we didn't find one.
     */
    private static String getCodepointFromFilename(String filename)
    {
        Matcher emojiFilenameMatcher = emojiFilenamePattern.matcher(filename);
        String match = null;

        if (emojiFilenameMatcher.find())
        {
            match = emojiFilenameMatcher.group(1);
        }

        return match;
    }

    /**
     * @param codepoint int representation of unicode codepoint
     * @return Filepath to find emoji picture of supplied codepoint
     */
    static String getURLFromCodepoint(int codepoint)
    {
        return getURLFromCodepoints(new ArrayList<>(Arrays.asList(codepoint)));
    }

    static String getURLFromCodepoints(List<Integer> codepoints)
    {
        return EMOJI_IMAGE_URL_PREFIX + getFilepathSuffixFromCodepoints(codepoints);
    }

    /**
     * @param codepoint int representation of unicode codepoint
     * @return Filepath to find emoji picture of supplied codepoint
     */
    static String getFilepathFromCodepoint(int codepoint)
    {
        return getFilepathFromCodepoints(new ArrayList<>(Arrays.asList(codepoint)));
    }

    @VisibleForTesting
    public static String getFilepathFromCodepoints(List<Integer> codepoints)
    {
        return EMOJI_IMAGE_DIR + "/emoji_u" + getFilepathSuffixFromCodepoints(codepoints);
    }

    public static String getFilepathSuffixFromCodepoints(List<Integer> codepoints)
    {
        StringBuilder filepathBuilder = new StringBuilder();

        String divider = "";
        for (Integer i : codepoints)
        {
            filepathBuilder.append(divider);
            divider = "_";
            filepathBuilder.append(String.format("%04x", i));
        }

        filepathBuilder.append(".png");

        return filepathBuilder.toString();
    }

    /**
     * Returns the set of codepoints of supported emojis as decimal Integers
     * @return set of codepoints of supported emojis
     */
    public static synchronized Set<Integer> getSetOfEmojiCodepointsNotStartingSequence()
    {
        if (setOfEmojiCodepointsNotStartingSequence != null)
        {
            return setOfEmojiCodepointsNotStartingSequence;
        }

        parseEmojiFiles();
        return setOfEmojiCodepointsNotStartingSequence;
    }

    /**
     * Returns the set of all codepoint sequences for all supported emojis,
     * including single codepoint ones.
     */
    public static synchronized Set<List<Integer>> getCodepointSequences()
    {
        if (setOfEmojiCodepointSequences != null)
        {
            return setOfEmojiCodepointSequences;
        }

        parseEmojiFiles();

        return setOfEmojiCodepointSequences;
    }

    private static void parseEmojiFiles()
    {
        setOfEmojiCodepointsNotStartingSequence = new HashSet<>();
        setOfEmojiCodepointSequences = new HashSet<>();
        Set<Integer> setOfMultipleCodepointFirsts = new HashSet<>();

        List<String> emojiUrls = EmojiActivator.getResources().getUrlsFromDirectory(EMOJI_IMAGE_DIR, "*.png");

        for (String filename : emojiUrls)
        {
            String hexCodepointOfFile = getCodepointFromFilename(filename);

            if (hexCodepointOfFile == null)
            {
                continue;
            }
            String[] codepoints = hexCodepointOfFile.split("_");

            if (codepoints.length == 0)
            {
                sLog.warn("Failed to parse codepoints from " + filename);
                continue;
            }

            List<Integer> codepointSequence = new ArrayList<>();
            for (String codepoint : codepoints)
            {
                codepointSequence.add(Integer.parseInt(codepoint, 16));
            }

            setOfEmojiCodepointSequences.add(codepointSequence);

            if (codepointSequence.size() == 1)
            {
                setOfEmojiCodepointsNotStartingSequence.add(codepointSequence.get(0));
            }
            else
            {
                setOfMultipleCodepointFirsts.add(codepointSequence.get(0));
            }
        }

        // Make sure setOfEmojiCodepointsNotStartingSequence only has codepoints that don't
        // start longer sequences. Any single codepoint emojis that also start
        // sequences are in setOfEmojiCodepointSequences - this extra Set is
        // just for quicker matching.
        for (Integer codepoint: setOfMultipleCodepointFirsts)
        {
            setOfEmojiCodepointsNotStartingSequence.remove(codepoint);
        }
    }

    /**
     * Returns the list of emoji tabs
     * @return list of emoji tabs
     */
    static List<TabOfInsertableIcons> getTabs()
    {
        return listOfTabs;
    }

    /**
     * Loads all the emoji tabs and then adds them to the listOfTabs
     */
    static void loadEmojiImages()
    {
        sLog.debug("Beginning process of creating emoji tabs");

        listOfTabs.add(getSmileysAndPeopleTab());
        listOfTabs.add(getAnimalsAndNatureTab());
        listOfTabs.add(getFoodAndDrinkTab());
        listOfTabs.add(getActivitiesTab());
        listOfTabs.add(getTravelAndPlacesTab());
        listOfTabs.add(getObjectsTab());
        listOfTabs.add(getSymbolsTab());

        mAllEmojisLoaded = true;
        sLog.debug("Created all the emoji tabs");
    }

   /**
    * @return true once and only once all emojis have been loaded
    */
    static boolean allEmojisLoaded()
    {
        return mAllEmojisLoaded;
    }

    public static String createURLFromImagePath(String ImagePath){
        return EMOJI_IMAGE_URL_PREFIX + ImagePath;
    }
}
