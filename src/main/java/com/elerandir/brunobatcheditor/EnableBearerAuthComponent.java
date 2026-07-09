package com.elerandir.brunobatcheditor;

import com.elerandir.brunobatcheditor.model.EnableBearerAuthConfig;
import dagger.BindsInstance;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component
public interface EnableBearerAuthComponent {

    BruParser bruParser();

    BearerAuthEnabler bearerAuthEnabler();

    @Component.Factory
    interface Factory {
        EnableBearerAuthComponent create(@BindsInstance EnableBearerAuthConfig config);
    }
}
