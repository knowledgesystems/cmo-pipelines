# import-scripts

A collection of the scripts in use across multiple production import servers.

We try to make these scripts flexible in order to be usable in multiple environments. So some scripts work in conjunction with a configuration file which must be provided as a command line argument or to be installed persistently on the server where the script will run.

In addition to the scripts, this directory contains test code and test data in subdirectories (e.g. test and test-py3). Some server-specific code and the expected contents of server crontabs are in server specific subdirectories (e.g. pipelines_eks and knowledgesystems-importer). Additional special purpose subdirectories contain archived code which were deposited here for reference.

## descriptions and notes

### kubernetes authentication

To automate operations in our kubernetes clusters, there are a number of service users created within AWS for that purpose. The users are authenticated through the AWS IAM service, and do not require normal two-factor authentication such as PingID. We use off the shelf utility saml2aws to generate an authorization token which is used with kubectl to manipulate kubernetes cluster resources.

Conceptually, at the top level is AWS, a service provider. The next level is the account level - accounts are accessed by users through the IAM service. The next level is the cluster level where we have created several kubernetes clusters per account using EKS. Within a cluser, we can manipulate resources (services, deployments, pods, ...) through kubectl.

AWS accounts are configured for different use cases. One of our AWS account has a network configuration within the DMZ which is not directly reachable from the outside world but which can open connections to servers within the MSKCC network. A different account which has a network configuration which cannot open connections to servers within the MSKCC network but which can receive direct connections from the outside world.

Separate EKS clusters might be created to separate development from production activities, or for migration purposes.

#### authenticate_service_account.sh

The script authenticate_service_account.sh works at the account level. There are two accounts which are currently configured for use, and these accounts can be referred to with the simplified identifiers "public" and "eks".

```
usage: authenticate_service_account.sh cluster_account_id [config_filepath]
    valid cluster_account_id values seem to be:
    - public (account 203403084713)
    - eks (account 666628074417)
    config_filepath defaults to '/data/portal-cron/pipelines-credentials/authenticate_service_account.conf'
```

The config_filepath reference contains a mapping from the simplified account identifier to a file containing the account credentials for that account. The current configuration file we are using contains:

```
# format is two string values separated by a single <TAB>
# first value is cluster_account_id, second is credentials_filename
# Any line starting with # or empty after discarding leading whitespace is ignored
#
public	public-account.credentials
eks	eks-account.credentials
```

The credentials files contain 4 properties, formatted like this:
eks.account.number=aws_account_number_goes_here
eks.role=role_name_goes_here
eks.account.name=account_name_goes_here
eks.password=password_goes_here

When authenticate_service_account.sh runs it will store the returned authentication token in the file '~/.aws/credentials'. Tokens are stored under a named "profile". The name of the profile will be based on the simplified account identifier. "public" tokens are stored in the profile "automation_public", and "eks" tokens are stored in the profile "automation_eks". It is necessary to separate tokens by account so that if two import processes are running simultaneously on the same server using different accounts, the authentication step of one process will not overwrite the token which is in use by the other process.

Once the valid token is available in ~/.aws/credentials, the kubectl command can be used by configuring kubectl to use the aws authentication token to generate a kubernetes token (at time of need) for use with a particular EKS cluster. For each defined EKS cluster, we have stored a configuration file which connects the appropriate AWS user to the EKS cluster in which to execute kubectl operations. For example, using kubectl with the command line argument '--kubeconfig /data/portal-cron/pipelines-credentials/public-cluster-kubeconfig' will use the generated AWS authentiation token stored under AWS profile "automation_public" to generate a kuberenets cluster token valid for use with the cluster 'cbioportal-prod'.

The automated scripted operations of the import processes should always use kubectl with a selected kubeconfig file which connects to the intended cluster. However, for manual interactions by maintainers kubectl's default configuration can easily be configured with the script 'authenticate_and_configure_kubectl.sh' (see below).

#### authenticate_and_configure_kubectl.sh

The script authenticate_and_configure_kubectl.sh works at two levels. It uses script 'authenticate_service_account.sh' to create an AWS authentication token for a selected user, and then it copies the kubeconfig file for a selected cluster into the default location for kubectl commandline use (~/.kube/config). After that is done, manual command line use of the kubectl command will interact with the selected EKS cluster without needing to supply the --kubeconfig command line argument.

```
usage: authenticate_and_configure_kubectl.sh cluster_id [config_filepath]
    valid cluster_id values seem to be:
    - publicprod
    - eksprod
    - eksargocd
    - publicargocd
    config_filepath defaults to '/data/portal-cron/pipelines-credentials/authenticate_and_configure_kubectl.conf'
```

The main argument is a simplified name for the eks cluster which is intended to become the target for kubectl command operations. The associated AWS user can be inferred from the choice of cluster. This association between the simplified cluster name, the simplified AWS user name, and the associated kubeconfig file are stored in a configuration file formatted like this:

```
# format is three string values separated by <TAB>
# first value is cluster_id, second is cluster_account_id, third is the cluster's kube/config file
# Any line starting with # or empty after discarding leading whitespace is ignored
#
publicprod	public	public-cluster-kubeconfig
publicargocd	public	publicargocd-cluster-kubeconfig
eksprod	eks	eks-cluster-kubeconfig
eksargocd	eks	eksargocd-cluster-kubeconfig
```

### source data repository management

Source data repository management became more complex when the public portal importer had a need to import data housed within the MSKCC enterprise github server (such as from the 'private' and 'impact' data repositories) but there was no network path for the public importer ec2 instance to directly pull upates from that github server. The solution implemented was to do data fetching for imports cooperatively on two servers. A local fetch process on knowledgesystems-importer would fetch updates from accessible repos such as datahub. A remote fetch process on pipelines3 would fetch updates from the MSKCC enterprise github server and then rsync those updates onto knowledgesystems-importer.

In addition to the need just described, we have been gradually losing the ability to fetch github repository updates using our built java based importer jars. The importer jars perform a number of git and git lfs operations external to the JVM using subprocesses spawned from the JVM process - but one assumption was that we would always be fetching from the default branch and that the name of that branch would be 'master'. Github has changed the name of the default branch assigned automatically to newly created repositories to 'main', and several of our more newly created data repositories use that name for the default branch and cannot be fetch using the compiled jars. There is no inherent advantage to performing these git and git lfs executions from within a java program. The same sequence of operations for repository updates and for repsitory cleanup were coded into the script 'data_source_repo_clone_manager.sh'.

#### data_source_repo_clone_manager.sh

The script data_source_repo_clone_manager.sh operates in two modes : 'pull' mode which will update cloned data in a list of repositories to match the head of the default branch of each, and 'cleanup' mode which will discard any extra local files or updates which are not part of the data set matching the head of the the default branch for a list of repositories. Some data fetches which occur during a 'pull' execution may also be rsynced to a remote server when appropriate. To know whether fetched data is needed on the local importer ec2 instance or on a remote importer ec2 instance, a configuration file is used to define the set of importer hosts, the set of portal databases which could be importer targets, and the set of source data repositories which can be fetched from. Since some repositories are imported to multiple portal databases, the actual target database into which the import process will be loading data must be provided as a command line argument.

usage:
```
data_source_repo_clone_manager.sh config_filepath command destination_database_id repo_id...
    destination_database_id must be a database defined in the config file
    command can be 'pull' to reset mentioned repositories to a clean state on the default branch with all
       available updates applied, or can be 'cleanup' which cleans and prunes the mentioned repositories
    repo_id is one or more repository identifiers defined in the config file, or 'all_sources'
        to indicate that all valid data source repositories for the destination database should be updated.
```

config_filepath must refer to a valid config file for this script. What follows is a description of that config file and an example.

The file declares three lists of objects: hosts, repositories, and database_destinations

The hosts list assigns a simplified identifier for each host which might be used to update cbioportal studies (such as 'pipelines3'). Each host also has several defined properties:
- host_can_connect_to_msk_network can be "yes" or "no"
- host_self_reported_hostname is the string which is returned by the "hostname" command when run on the local system
- host_ssh_dns_hostname is a valid DNS hostname for opening connections to the host over the internet
- host_incoming_ssh_username is a username which can be used to open an ssh connection to the host in conjunction with a private key
- host_ssh_private_key_local_filepath is a filepath to the private key needed to open an ssh connection

The repositories list assigns a simplified identifier for each repository. (mostly we use the "data source" identifiers in use before). Each repository also has several defined properties:
- repository_clone_dirpath_per_host_map is a mapping from host names to the absolute filesystem path to the root directory under which data source repository clones are located for import operations.
- repository_local_path_for_remote_rsync is a directory on the local filesystem which is not used for import operations locally but is instead a temporary working space where a repository clone can be updated before rsyncing the updated data to a remote system. (since this functionality it currently only performed by pipelines3, this property is only set in the configuration file on pipelines3 ... the property can be marked as unset by using string 'none')
- repository_clone_limited_scope_subdirectory is a relative path beneath the repository_clone_dirpath for cases where we only want to fetch or use a portion of a repository clone. An example of this is the data source we named "knowledge-systems-curated-studies". Data fetches to that particular repository should only fetch or transfer the content of the subdirectory named "studies", and this setting limits the rsync operation to that subdirectory.

The database_destinations list assigns a simplified identifier to each database destination. Current examples are: 'public', 'genie', and 'msk', which refer to three of our primary cBioPortal databases used in production. Each database_destination has several defined properties:
- configured_importer_host is the simplified host identifier for the host which is responsible for updating studies in this database
- all_recognized_source_repositories is a list of simplified repository identifiers. These are all supported repositories in use on the importer host for importing into this database destination

The basic algorithm is to first determine whether the updates are needed on the local system or on a remote system based on the user-provided destination_database argument. Next, data is fetched for each named repository. It is done in the actual local clone location if the import will run on the local system, or in the repository_local_path_for_remote_rsync location if the import will run on a remote system. After fetching data which is needed on a remote system, the data will be sent to the remote system actual local clone location via rsync.

Because of the length of the [example config file](import-scripts/importer-data-source-manager-config.yaml.EXAMPLE), the link just given can be followed to examine the example.
