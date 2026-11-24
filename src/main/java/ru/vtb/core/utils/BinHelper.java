package ru.vtb.core.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.TEMP_FILES_FOLDER;

public class BinHelper {

    public static String generateBinFilename(String... mask) {
        List<String> masks = new ArrayList<>();
        masks.add("Binm");
        masks.add("Binv");
        masks.add("Bavi");
        masks.add("Bava");
        masks.add("Bavu");
        String buf = mask.length == 0 ? masks.get(ThreadLocalRandom.current().nextInt(masks.size())) : mask[0];

        return String.join("_", "0000", "1266", buf, "P",
                        CommonHelper.getCurrentDateInFormat("yyyyMMdd"), "66",
                        RandomStringUtils.insecure().nextAlphanumeric(10).toUpperCase())
                .concat(".xml");
    }

    public static File getBinFileReference(String filename, boolean isEmpty) {
        String resultPath = TEMP_FILES_FOLDER + FILE_SEPARATOR;
        String pathFrom = resultPath + "binsReference.xml";
        String pathTo = resultPath + filename;
        if (!isEmpty) {
            return FileHelper.copyFile(
                    pathFrom,
                    pathTo);
        } else {
            return FileHelper.writeFile(" ".getBytes(), pathTo);
        }
    }

    public static File getBinFileReference(String filename) {
        return getBinFileReference(filename, false);
    }
}
