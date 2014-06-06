package org.cstamas.nio2.files;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith( Parameterized.class )
public class AtomicMoveSupportTest
    extends TestSupport
{
    @Parameterized.Parameters(name = "{index}: targetExists={0}, crossVolume={1}, atomicMove={2}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][]{
            { false, false, false }, // 0
            { true, false, false }, // 1
            { false, true, false }, // 2
            { true, true, false }, // 3
            { false, false, true }, // 4
            { true, false, true }, // 5
            { false, true, true }, // 6
            { true, true, true } // 7
        } );
    }

    @Parameterized.Parameter( value = 0 )
    public boolean targetExists;

    @Parameterized.Parameter( value = 1 )
    public boolean crossVolume;

    @Parameterized.Parameter( value = 2 )
    public boolean atomicMove;

    private File volumeADirectory;

    private File volumeBDirectory;

    @Before
    public void before()
        throws IOException
    {
        volumeADirectory = util.createTempDir( new File( "/Users/cstamas/tmp" ), "volumeA" );
        if ( crossVolume )
        {
            volumeBDirectory = util.createTempDir( new File( "/Volumes/My Alu Book/tmp" ), "volumeB" );
        }
        else
        {
            volumeBDirectory = volumeADirectory;
        }
    }

    @After
    public void after()
        throws Exception
    {
        delete( volumeADirectory.toPath() );
        delete( volumeBDirectory.toPath() );
    }

    private void delete( final Path dir )
        throws IOException
    {
        if ( !Files.exists( dir ) )
        {
            return;
        }
        if ( Files.isDirectory( dir ) )
        {
            Files.walkFileTree( dir, ImmutableSet.of( FileVisitOption.FOLLOW_LINKS ), Integer.MAX_VALUE,
                                new SimpleFileVisitor<Path>()
                                {
                                    @Override
                                    public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs )
                                        throws IOException
                                    {
                                        Files.delete( file );
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult postVisitDirectory( final Path dir, final IOException exc )
                                        throws IOException
                                    {
                                        if ( exc != null )
                                        {
                                            throw exc;
                                        }
                                        else
                                        {
                                            Files.delete( dir );
                                            return FileVisitResult.CONTINUE;
                                        }
                                    }
                                } );
        }
        else
        {
            Files.delete( dir );
        }
    }

    private void writeTo( final File file, final String content )
        throws IOException
    {
        com.google.common.io.Files.write( content, file, Charsets.UTF_8 );
    }


    private void move( final Path from, final Path to )
        throws IOException
    {
        if (atomicMove) {
            Files.move( from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
        } else {
            Files.move( from, to, StandardCopyOption.REPLACE_EXISTING );
        }
    }

    private void perform( final Path from, final Path to )
        throws Exception
    {
        assertThat( from, notNullValue() );
        assertThat( "from must exists", Files.exists( from ), is( true ) );
        assertThat( from, notNullValue() );
        move( from, to );
    }

    // ==

    @Test
    public void singleFile()
        throws Exception
    {
        // from
        final File fromFile = util.createTempFile( volumeADirectory, "from" );
        writeTo( fromFile, "from" );

        // to
        final File toFile = util.createTempFile( volumeBDirectory, "to" );
        if ( targetExists )
        {
            writeTo( toFile, "to" );
        }

        perform( fromFile.toPath(), toFile.toPath() );
    }

    @Test
    public void emptyDirectory()
        throws Exception
    {
        // from
        final File fromFile = new File( volumeADirectory, "dir1" );
        fromFile.mkdirs();
        // to
        final File toFile = new File( volumeBDirectory, "dir2" );
        if ( targetExists )
        {
            toFile.mkdirs();
        }

        perform( fromFile.toPath(), toFile.toPath() );
    }

    @Test
    public void directoryStructure()
        throws Exception
    {
        // from
        final File fromFile = new File( volumeADirectory, "dir1" );
        fromFile.mkdirs();
        writeTo( new File( fromFile, "file1" ), "file1" );
        writeTo( new File( fromFile, "file2" ), "file2" );
        final File child = new File( fromFile, "child" );
        child.mkdirs();
        writeTo( new File( child, "file1" ), "file1" );
        // to
        final File toFile = new File( volumeBDirectory, "dir2" );
        if ( targetExists )
        {
            toFile.mkdirs();
        }

        perform( fromFile.toPath(), toFile.toPath() );
    }
}
