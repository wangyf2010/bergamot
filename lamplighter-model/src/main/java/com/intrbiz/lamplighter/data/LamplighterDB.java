package com.intrbiz.lamplighter.data;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.intrbiz.data.DataManager;
import com.intrbiz.data.cache.Cache;
import com.intrbiz.data.db.DatabaseAdapter;
import com.intrbiz.data.db.DatabaseConnection;
import com.intrbiz.data.db.compiler.DatabaseAdapterCompiler;
import com.intrbiz.data.db.compiler.meta.SQLGetter;
import com.intrbiz.data.db.compiler.meta.SQLParam;
import com.intrbiz.data.db.compiler.meta.SQLRemove;
import com.intrbiz.data.db.compiler.meta.SQLSchema;
import com.intrbiz.data.db.compiler.meta.SQLSetter;
import com.intrbiz.data.db.compiler.meta.SQLVersion;
import com.intrbiz.lamplighter.model.CheckReading;

@SQLSchema(
        name = "lamplighter", 
        version = @SQLVersion({1, 0, 0}),
        tables = {
            CheckReading.class
        }
)
public abstract class LamplighterDB extends DatabaseAdapter
{

    /**
     * Compile and register the Bergamot Database Adapter
     */
    static
    {
        DataManager.getInstance().registerDatabaseAdapter(
                LamplighterDB.class, 
                DatabaseAdapterCompiler.defaultPGSQLCompiler().compileAdapterFactory(LamplighterDB.class)
        );
    }
    
    public static void load()
    {
        // do nothing
    }
    
    /**
     * Install the Bergamot schema into the default database
     */
    public static void install()
    {
        Logger logger = Logger.getLogger(LamplighterDB.class);
        DatabaseConnection database = DataManager.getInstance().connect();
        DatabaseAdapterCompiler compiler =  DatabaseAdapterCompiler.defaultPGSQLCompiler().setDefaultOwner("bergamot");
        // check if the schema is installed
        if (! compiler.isSchemaInstalled(database, LamplighterDB.class))
        {
            logger.info("Installing database schema");
            compiler.installSchema(database, LamplighterDB.class);
        }
        else
        {
            // check the installed schema is upto date
            if (! compiler.isSchemaUptoDate(database, LamplighterDB.class))
            {
                logger.info("The installed database schema is not upto date");
                compiler.upgradeSchema(database, LamplighterDB.class);
            }
            else
            {
                logger.info("The installed database schema is upto date");
            }
        }
    }

    /**
     * Connect to the Bergamot database
     */
    public static LamplighterDB connect()
    {
        return DataManager.getInstance().databaseAdapter(LamplighterDB.class);
    }
    
    /**
     * Connect to the Bergamot database
     */
    public static LamplighterDB connect(DatabaseConnection connection)
    {
        return DataManager.getInstance().databaseAdapter(LamplighterDB.class, connection);
    }

    public LamplighterDB(DatabaseConnection connection, Cache cache)
    {
        super(connection, cache);
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length == 1 && "install".equals(args[0]))
        {
            DatabaseAdapterCompiler.main(new String[] { "install", LamplighterDB.class.getCanonicalName() });
        }
        else if (args.length == 2 && "upgrade".equals(args[0]))
        {
            DatabaseAdapterCompiler.main(new String[] { "upgrade", LamplighterDB.class.getCanonicalName(), args[1] });
        }
        else
        {
            // interactive
            try (Scanner input = new Scanner(System.in))
            {
                for (;;)
                {
                    System.out.print("Would you like to generate the install or upgrade schema: ");
                    String action = input.nextLine();
                    // process the action
                    if ("exit".equals(action) || "quit".equals(action) || "q".equals(action))
                    {
                        System.exit(0);
                    }
                    else if ("install".equalsIgnoreCase(action) || "in".equalsIgnoreCase(action) || "i".equalsIgnoreCase(action))
                    {
                        DatabaseAdapterCompiler.main(new String[] { "install", LamplighterDB.class.getCanonicalName() });
                        System.exit(0);
                    }
                    else if ("upgrade".equalsIgnoreCase(action) || "up".equalsIgnoreCase(action) || "u".equalsIgnoreCase(action))
                    {
                        System.out.print("What is the current installed version: ");
                        String version = input.nextLine();
                        DatabaseAdapterCompiler.main(new String[] { "upgrade", LamplighterDB.class.getCanonicalName(), version });
                        System.exit(0);
                    }
                }
            }
        }
    }
    
    // reading metadata
    
    @SQLSetter(table = CheckReading.class, name = "set_check_reading", since = @SQLVersion({1, 0, 0}))
    public abstract void setCheckReading(CheckReading reading);
    
    @SQLGetter(table = CheckReading.class, name = "get_check_reading", since = @SQLVersion({1, 0, 0}))
    public abstract CheckReading getCheckReading(@SQLParam("id") UUID id);
    
    @SQLGetter(table = CheckReading.class, name ="get_check_reading_by_name", since = @SQLVersion({1, 0, 0}))
    public abstract CheckReading getCheckReadingByName(@SQLParam("check_id") UUID checkId, @SQLParam("name") String name);
    
    @SQLGetter(table = CheckReading.class, name ="get_check_readings_for_check", since = @SQLVersion({1, 0, 0}))
    public abstract List<CheckReading> getCheckReadingsForCheck(@SQLParam("check_id") UUID checkId);
    
    @SQLRemove(table = CheckReading.class, name = "remove_check_reading", since = @SQLVersion({1, 0, 0}))
    public abstract void removeCheckReading(@SQLParam("id") UUID id);
    
    @SQLGetter(table = CheckReading.class, name = "list_check_readings", since = @SQLVersion({1, 0, 0}))
    public abstract List<CheckReading> listCheckReadings();
    
    // reading management
    
    // gauges
    
    
}