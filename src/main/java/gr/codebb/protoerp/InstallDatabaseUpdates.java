/*
 * copyright 2013-2020
 * codebb.gr
 * ProtoERP - Open source invocing program
 * info@codebb.gr
 */
/*
 * Changelog
 * =========
 * 14/10/2020 (georgemoralis) - Initial
 */
package gr.codebb.protoerp;

import static gr.codebb.lib.util.ThreadUtil.runAndWait;

import gr.codebb.dlg.AlertDlg;
import gr.codebb.lib.database.MysqlUtil;
import gr.codebb.lib.database.PersistenceManager;
import gr.codebb.util.database.Dbms;
import gr.codebb.util.database.SqlScriptRunner;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.stage.Modality;
import javax.persistence.EntityManager;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.internal.SessionImpl;

public class InstallDatabaseUpdates {

  public static boolean checkDatabaseForUpdates(Dbms database) {
    InstallDatabaseUpdates d = new InstallDatabaseUpdates();
    // if table not exists then run the initial database script
    boolean newDatabase = false;
    if (!MysqlUtil.checkDatabaseConnection(database)) {
      return false;
    }
    if (!MysqlUtil.checkIfTableExists(database, "settings")) {
      EntityManager em = PersistenceManager.getEmf().createEntityManager();
      Connection connection = em.unwrap(SessionImpl.class).connection();
      d.install_initial_database(connection);
      em.close();
      newDatabase = true;
    }
    d.runUpdates(database, newDatabase);

    if (newDatabase) {
      try {
        /** we are still in preloader thread so wait to finish */
        runAndWait(
            () -> {
              AlertDlg.create()
                  .type(AlertDlg.Type.INFORMATION)
                  .message("Δεν βρέθηκε βάση,αρχικοποιήθηκε μία καινούρια")
                  .title("Ειδοποίηση")
                  .owner(null)
                  .modality(Modality.APPLICATION_MODAL)
                  .showAndWait();
            });
      } catch (InterruptedException | ExecutionException ex) {
        ex.printStackTrace();
      }
    }
    return true;
  }

  public void runUpdates(Dbms db, boolean newDatabase) {
    java.sql.Connection connection = null;
    try {

      // Getting a connection to the newly started database
      Class.forName("com.mysql.cj.jdbc.Driver");
      try {
        connection = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword());
      } catch (SQLException ex) {
        ex.printStackTrace();
      }

    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }
    if (connection == null) {
      return;
    }
    System.out.println("Starting database update");
    Database database = null;
    try {
      database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    } catch (DatabaseException ex) {
      ex.printStackTrace();
    }
    if (database == null) {
      try {
        connection.close();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
      // TODO error message?
      return;
    }
    try {
      Liquibase liquibase =
          new liquibase.Liquibase(
              "dbupdates/master-changelog.xml", new ClassLoaderResourceAccessor(), database);
      List<ChangeSet> changeSets = liquibase.listUnrunChangeSets((Contexts) null);
      if (!changeSets.isEmpty()) {
        try {
          liquibase.update((Contexts) null);
        } catch (Exception d) {
          /*runAndWait( //TODO
          () -> {
            Alert alert =
                AlertHelper.ConfirmDialog(
                    "Δημιουργήθηκε κάποιο πρόβλημα κατά την αναβάθμιση της βάσης\nΕπιθυμείτε να στείλετε το report στην codebb για περαιτέρω διερεύνηση?",
                    "Prototype",
                    Modality.APPLICATION_MODAL,
                    null);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
              String exceptionText = ExceptionUtil.exceptionToString(d);
              FxmlUtil.LoadResult<GenericIssueController> GenericIssue =
                  FxmlUtil.load("/fxml/fxGenericIssue.fxml");
              GenericIssue.getController().setTitle("AutoGenerated database update issue");
              GenericIssue.getController().setDetail(exceptionText);
              Stage stage =
                  StageUtil.setStageSettings(
                      "Αποστολή προτάσεων/προβλημάτων",
                      new Scene(GenericIssue.getParent()),
                      Modality.APPLICATION_MODAL,
                      null,
                      null,
                      "/img/protoerp.png");
              stage.setResizable(false);
              stage.setAlwaysOnTop(true);
              stage.sizeToScene();
              stage.show();
            }
          });*/
          try {
            connection.close();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
          return;
        }
      } else {
        System.out.println("No database updates found");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Completed database update");
    try {
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  public void install_initial_database(Connection connection) {
    System.out.println(
        "[DatabaseEvents]: Table " + "settings" + " does not exist. Creating database layout.");
    SqlScriptRunner runner = new SqlScriptRunner(connection, false, false);
    try {
      String file = "/" + MainSettings.getInstance().getAppName() + "_initial.sql";
      runner.runScript(new InputStreamReader(getClass().getResourceAsStream(file), "UTF-8"));
    } catch (IOException | SQLException ex) {
      ex.printStackTrace();
    }
  }
}
