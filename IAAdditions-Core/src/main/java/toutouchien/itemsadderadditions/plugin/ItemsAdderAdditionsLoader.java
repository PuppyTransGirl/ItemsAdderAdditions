package toutouchien.itemsadderadditions.plugin;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public class ItemsAdderAdditionsLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

/*        try {
            resolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        } catch (NoSuchFieldError err) {
            resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://maven-central.storage-download.googleapis.com/maven2").build());
        }*/

        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("net.bytebuddy:byte-buddy-agent:1.18.8"), null));

        classpathBuilder.addLibrary(resolver);
    }
}
