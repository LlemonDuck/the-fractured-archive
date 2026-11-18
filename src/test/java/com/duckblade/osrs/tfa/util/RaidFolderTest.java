package com.duckblade.osrs.tfa.util;

import com.duckblade.osrs.tfa.TfaPlugin;
import java.nio.file.Path;
import net.runelite.client.util.Filepath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaidFolderTest
{

	@Mock
	TfaPlugin plugin;

	@TempDir
	Path dataDirectory;

	RaidFolder raidFolder;

	@BeforeEach
	void setUp() throws Exception
	{
		when(plugin.getDataDirectory()).thenReturn(Filepath.Unchecked.getRooted(dataDirectory));
		raidFolder = new RaidFolder(plugin);
	}

	@Test
	void pathIsComputedWithoutCreatingTheFolderOnStartUp()
	{
		assertNull(raidFolder.getPath());

		raidFolder.startUp();

		Filepath path = raidFolder.getPath();
		assertNotNull(path);
		assertEquals(dataDirectory.toAbsolutePath().normalize(), Filepath.Unchecked.getPath(path).getParent());
		assertFalse(path.exists());
	}

	@Test
	void pathIsUnsetOnShutDown()
	{
		raidFolder.startUp();
		raidFolder.shutDown();

		assertNull(raidFolder.getPath());
	}

}
