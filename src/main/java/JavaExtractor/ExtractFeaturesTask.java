package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.ProgramFeatures;
import com.github.javaparser.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ExtractFeaturesTask implements Callable<Void> {
    CommandLineValues m_CommandLineValues;
    Path filePath;

    public ExtractFeaturesTask(CommandLineValues commandLineValues, Path path) {
        m_CommandLineValues = commandLineValues;
        this.filePath = path;
    }

    @Override
    public Void call() throws Exception {
        //System.err.println("Extracting file: " + filePath);
        processFile();
        //System.err.println("Done with file: " + filePath);
        return null;
    }

    public void sendData(ArrayList<String> messages) throws IOException {
        // need host and port, we want to connect to the ServerSocket at port 7777
        Socket socket = new Socket("localhost", 7777);
        System.out.println("Connected!");

        // get the output stream from the socket.
        OutputStream outputStream = socket.getOutputStream();
        // create an object output stream from the output stream so we can send an object through it
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

        System.out.println("Sending messages to the ServerSocket");
        objectOutputStream.writeObject(messages);

        System.out.println("Closing socket and terminating program.");
        socket.close();
    }

    public void processFile() {
        ArrayList<ProgramFeatures> features;
        try {
            features = extractSingleFile();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return;
        }
        if (features == null) {
            return;
        }
        for (ProgramFeatures pf : features) {
            try {
                System.out.println(pf.getName() + "," + pf.getFeatures().size() + ","
						+ this.filePath.toAbsolutePath().toString().split("raw_java")[1]);
            } catch (Exception e) {
                System.out.println(pf.getName() + "," + pf.getFeatures().size() + ","
						+ this.filePath.toAbsolutePath());
            }
        }
    }

    public ArrayList<MethodAST> getMethodsAndAsts() {
        ArrayList<ProgramFeatures> features;
        try {
            features = extractSingleFile();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        if (features == null) {
            return new ArrayList<>();
        }
        ArrayList<MethodAST> filesMethods = new ArrayList<>();

        for (ProgramFeatures pf : features) {
            filesMethods.add(new MethodAST(pf.getName(), pf.getFeatures().size()));
        }

        return (filesMethods);
    }

    public ArrayList<ProgramFeatures> extractSingleFile() throws ParseException, IOException {
        String code = null;
        try {
            code = new String(Files.readAllBytes(this.filePath));
        } catch (IOException e) {
            e.printStackTrace();
            code = Common.EmptyString;
        }
        FeatureExtractor featureExtractor = new FeatureExtractor(m_CommandLineValues);

        ArrayList<ProgramFeatures> features = featureExtractor.extractFeatures(code);

        return features;
    }

    public String featuresToString(ArrayList<ProgramFeatures> features) {
        if (features == null || features.isEmpty()) {
            return Common.EmptyString;
        }

        List<String> methodsOutputs = new ArrayList<>();

        for (ProgramFeatures singleMethodfeatures : features) {
            StringBuilder builder = new StringBuilder();

            String toPrint = Common.EmptyString;
            toPrint = singleMethodfeatures.toString();
            if (m_CommandLineValues.PrettyPrint) {
                toPrint = toPrint.replace(" ", "\n\t");
            }
            builder.append(toPrint);

            methodsOutputs.add(builder.toString());
        }
        return StringUtils.join(methodsOutputs, "\n");
    }
}
