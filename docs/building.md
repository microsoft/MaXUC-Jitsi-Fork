# Building jitsi
The project is built using Apache Ant tool and Apache Ivy as a dependency manager.

Additional 3rd party Java libraries are required in order to build the project. 

The dependencies can be downloaded from a Maven repository:
1. Create `ivysettings.xml` file with definitions of dependency resolvers. E.g. to download artefacts from Maven Central it can look like:
```ivysettings.xml
<ivy-settings>
    <settings defaultResolver="chain-resolver" />
    <resolvers>
        <chain name="chain-resolver" returnFirst="true">
            <!-- First check local folder -->
            <filesystem name="libraries">
                <artifact pattern="${sc.basedir}/lib/installer-exclude/[artifact].[ext]"/>
            </filesystem>
            <ibiblio name="maven" m2compatible="true" root="https://repo1.maven.org/maven2/" />
        </chain>
    </resolvers>
</ivy-settings>
```
2. Not all required dependencies are available in Maven. Download, build and put the following libraries in the `lib` folder:
- fmj.jar - built from source https://github.com/microsoft/MaXUC-FMJ-Fork/
- libjitsi.jar - build from source https://github.com/microsoft/MaXUC-LibJitsi-Fork/
- profiler4j-1.0-beta3-SC.jar - build from source https://sourceforge.net/projects/profiler4j/
3. Run `ant make package` command which will download Maven dependencies and build output artefacts to `sc-bundles`.

## Customisation changes made to dependencies 
The project uses several customised versions of public libraries. The following changes were made: 
- jain-si-ri (based on https://github.com/usnistgov/jsip):
    - added enum class `gov.nist.javax.sip.header.ims.SecurityMechanism` storing available security types for use in SIP Security-Client.
    - added inner enum class `gov.nist.core.StackLogger.Direction` to represent direction of SIP message stream.
    - added an instance property of type `javax.sip.address.Hop` in `gov.nist.javax.sip.header.Route`.
    - added a new method `setRegistrar` to an interface `javax.sip.SipProvider` to update registrar value.
    - added overloaded methods `logFatalXXX(String message, Throwable cause)` to `gov.nist.core.StackLogger` to expose the cause value.
- Smack (based on https://github.com/igniterealtime/Smack/):
    - implemented XEP-0313 Message Archive Management.
    - added method `getSocket()` in `org.jivesoftware.smack.XMPPConnection` to expose currently used socket property.
    - added interface `org.jivesoftware.smack.PasswordFuture` as a wrapper for password value object.
    - added a new class `org.jivesoftware.smackx.debugger.xmpp.XmppDebugger` implementing the interface `org.jivesoftware.smack.debugger.SmackDebugger` to log XMPP stream.
    - added a new method `createItemAndRequestSubscription` to `org.jivesoftware.smack.roster.Roster` for convenience.
    - added a new method `loadVCard(EntityBareJid,long)` to `org.jivesoftware.smackx.vcardtemp.VCardManager` for convenience.
    - added a new method `inviteDirectly(EntityBareJid)` to `org.jivesoftware.smackx.muc.MultiUserChat` to invite the specified participant directly.
    - added a new method `getStreamInitiation()` to `org.jivesoftware.smackx.filetransfer.FileTransferRequest` to expose stream initiation object.
 