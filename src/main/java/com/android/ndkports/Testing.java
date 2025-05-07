package com.android.ndkports;

import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class Testing {

    // Base TestResult class
    public static abstract class TestResult {
        @Getter
        private final String name;
        @Getter
        private final Abi abi;

        protected TestResult(String name, Abi abi) {
            this.name = name;
            this.abi = abi;
        }

        // Success subclass
        public static class Success extends TestResult {
            public Success(String name, Abi abi) {
                super(name, abi);
            }

            @Override
            public String toString() {
                return "PASS " + getAbi() + " " + getName();
            }
        }

        // Failure subclass
        public static class Failure extends TestResult {
            private final String output;

            public Failure(String name, Abi abi, String output) {
                super(name, abi);
                this.output = output;
            }

            @Override
            public String toString() {
                return "FAIL " + getAbi() + " " + getName() + ": " + output;
            }
        }
    }

    // PushSpec class
    public static class PushSpec {
        @Getter
        private final File src;
        @Getter
        private final File dest;

        public PushSpec(File src, File dest) {
            this.src = src;
            this.dest = dest;
        }
    }

    // PushBuilder class
    public static class PushBuilder {
        @Getter
        private final Abi abi;
        @Getter
        private final Toolchain toolchain;
        @Getter
        private final List<PushSpec> pushSpecs = new ArrayList<>();

        public PushBuilder(Abi abi, Toolchain toolchain) {
            this.abi = abi;
            this.toolchain = toolchain;
        }

        public void push(File src, File dest) {
            pushSpecs.add(new PushSpec(src, dest));
        }
    }

    // ShellTestSpec class
    public static class ShellTestSpec {
        @Getter
        private final String name;
        @Getter
        private final List<String> cmd;

        public ShellTestSpec(String name, List<String> cmd) {
            this.name = name;
            this.cmd = cmd;
        }
    }

    // ShellTestBuilder class
    public static class ShellTestBuilder {
        @Getter
        private final File deviceDirectory;
        @Getter
        private final Abi abi;
        @Getter
        private final List<ShellTestSpec> runSpecs = new ArrayList<>();

        public ShellTestBuilder(File deviceDirectory, Abi abi) {
            this.deviceDirectory = deviceDirectory;
            this.abi = abi;
        }

        public void shellTest(String name, List<String> cmd) {
            runSpecs.add(new ShellTestSpec(name, cmd));
        }
    }

    // Base path for device directory
    private static final File BASE_DEVICE_DIRECTORY = new File("/data/local/tmp/ndkports");

    // AndroidExecutableTestTask class
    public static abstract class AndroidExecutableTestTask extends DefaultTask {

        @InputDirectory
        public abstract DirectoryProperty getNdkPath();

        private Ndk getNdk() {
            return new Ndk(getNdkPath().getAsFile().get());
        }

        @Input
        public abstract Property<Integer> getMinSdkVersion();

        @Input
        public abstract Property<Consumer<PushBuilder>> getPush();

        public void push(Consumer<PushBuilder> block) {
            getPush().set(block);
        }

        @Input
        public abstract Property<Consumer<ShellTestBuilder>> getRun();

        public void run(Consumer<ShellTestBuilder> block) {
            getRun().set(block);
        }

        private File deviceDirectoryForAbi(Abi abi) {
            return new File(BASE_DEVICE_DIRECTORY,
                    getProject().getName() + "/" + abi.toString());
        }

        private void runTests(Device device, Abi abi, List<TestResult> results) throws Exception {
            File deviceDirectory = deviceDirectoryForAbi(abi);

            Consumer<PushBuilder> pushBlock = getPush().get();
            Consumer<ShellTestBuilder> runBlock = getRun().get();

            PushBuilder pushBuilder = new PushBuilder(abi,
                    new Toolchain(getNdk(), abi, getMinSdkVersion().get()));
            pushBlock.accept(pushBuilder);

            // Push all files in parallel
            List<CompletableFuture<Void>> pushFutures = pushBuilder.getPushSpecs().stream()
                    .map(spec -> CompletableFuture.runAsync(() -> {
                        try {
                            device.push(spec.getSrc(),
                                    new File(deviceDirectory, spec.getDest().getPath()));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .collect(Collectors.toList());

            // Wait for all pushes to complete
            CompletableFuture.allOf(pushFutures.toArray(new CompletableFuture[0])).get();

            ShellTestBuilder runBuilder = new ShellTestBuilder(deviceDirectory, abi);
            runBlock.accept(runBuilder);

            // Run tests in parallel
            List<CompletableFuture<TestResult>> testFutures = runBuilder.getRunSpecs().stream()
                    .map(spec -> CompletableFuture.supplyAsync(() -> {
                        try {
                            device.shell(spec.getCmd());
                            return new TestResult.Success(spec.getName(), abi);
                        } catch (AdbException ex) {
                            return new TestResult.Failure(spec.getName(), abi,
                                    ex.getCmd() + "\n" + ex.getOutput());
                        } catch (Exception e) {
                            return new TestResult.Failure(spec.getName(), abi, e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());

            // Wait for all tests to complete and collect results
            for (CompletableFuture<TestResult> future : testFutures) {
                results.add(future.get());
            }
        }

        @TaskAction
        public void runTask() {
            DeviceFleet fleet = new DeviceFleet();
            List<String> warnings = new ArrayList<>();
            List<TestResult> results = new ArrayList<>();

            List<CompletableFuture<Void>> abiTasks = new ArrayList<>();

            for (Abi abi : Abi.values()) {
                abiTasks.add(CompletableFuture.runAsync(() -> {
                    try {
                        Device device = fleet.findDeviceFor(
                                abi, abi.adjustMinSdkVersion(getMinSdkVersion().get()));
                        if (device == null) {
                            warnings.add("No device capable of running tests for " + abi + " " +
                                    "minSdkVersion " + getMinSdkVersion().get());
                            return;
                        }

                        device.shell(List.of("rm", "-rf", deviceDirectoryForAbi(abi).toString()));
                        runTests(device, abi, results);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            try {
                CompletableFuture.allOf(abiTasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            // Log warnings
            for (String warning : warnings) {
                getLogger().warn(warning);
            }

            // Check for failures
            List<TestResult.Failure> failures = results.stream()
                    .filter(TestResult.Failure.class::isInstance)
                    .map(TestResult.Failure.class::cast)
                    .collect(Collectors.toList());

            if (!failures.isEmpty()) {
                throw new RuntimeException("Tests failed:\n" +
                        failures.stream().map(Object::toString).collect(Collectors.joining("\n")));
            }
        }
    }
}