package com.viaversion.minestom.platform;

import com.viaversion.viaversion.api.platform.PlatformTask;
import net.minestom.server.timer.Task;

public record MinestomViaTask(Task task) implements PlatformTask<Task> {

    @Override
    public void cancel() {
        task.cancel();
    }
}
