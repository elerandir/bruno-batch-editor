package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.RuntimeConfig;
import dagger.BindsInstance;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component
public interface AppComponent {

    BruParser bruParser();

    BodyBlockReplacer bodyBlockReplacer();

    BruFileLocator bruFileLocator();

    @Component.Factory
    interface Factory {
        AppComponent create(@BindsInstance RuntimeConfig runtimeConfig);
    }
}
