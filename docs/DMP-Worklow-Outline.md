# DMP-Workflow-Outline

## 0. Pre-Update Steps
**********************************************************************
**Remove existing tmp files from $MSK_DMP_TMPDIR**
**Check environment variables**
**Refresh CDD Cache**
**Update DMP Git repository**

## 1. REDCap Exports
**********************************************************************
**REDCAP | Export Clinical Supp Date Added REDCap projects**
    *MSKIMPACT*
    *HEMEPACT*
    *RAINDANCE*
    *ARCHER*
    *ACCESS*
**REDCAP | Export CVR Clinical REDCap projects**
    *MSKIMPACT*
    *HEMEPACT*
    *RAINDANCE*
    *ARCHER*
    *ACCESS*

    ***[GITHUB | DMP Repository State: 0](#github-dmp-repository-state-0)***

## 2. MSKIMPACT Updates
**********************************************************************
**DARWIN CAISIS GBM CLINICAL AND TIMELINE FETCH**

    ***[GITHUB | DMP Repository State: 1](#github-dmp-repository-state-1)***

**CVR CLINICAL AND GENOMIC FETCH**

    ***[GITHUB | DMP Repository State: 2](#github-dmp-repository-state-2)***

**CVR GERMLINE GENOMIC FETCH**

    ***[GITHUB | DMP Repository State: 3](#github-dmp-repository-state-3)***

**DDP DEMOGRAPHICS DATA FETCH**

    ***[GITHUB | DMP Repository State: 4](#github-dmp-repository-state-4)***

**DDP DEMOGRAPHICS DATA FETCH FOR PEDIATRIC PATIENTS**

    ***[GITHUB | DMP Repository State: 5](#github-dmp-repository-state-5)***

**CRDB FETCH**


## 3. HEMEPACT Updates
**********************************************************************
**CVR CLINICAL AND GENOMIC FETCH**
    ***[GITHUB | DMP Repository State: 6](#github-dmp-repository-state-6)***

**DDP DEMOGRAPHICS DATA FETCH**
    ***[GITHUB | DMP Repository State: 7](#github-dmp-repository-state-7)***


## 4. RAINDANCE Updates
**********************************************************************
**CVR CLINICAL AND GENOMIC FETCH**
    ***[GITHUB | DMP Repository State: 8](#github-dmp-repository-state-8)***

**DDP DEMOGRAPHICS DATA FETCH**
    ***[GITHUB | DMP Repository State: 9](#github-dmp-repository-state-9)***


## 4. ARCHER Updates
**********************************************************************
**CVR CLINICAL AND GENOMIC FETCH**
    ***[GITHUB | DMP Repository State: 10](#github-dmp-repository-state-10)***

**DDP DEMOGRAPHICS DATA FETCH**
    ***[GITHUB | DMP Repository State: 11](#github-dmp-repository-state-11)***


## 5. ACCESS Updates
**********************************************************************
**CVR CLINICAL AND GENOMIC FETCH**
    ***[GITHUB | DMP Repository State: 12](#github-dmp-repository-state-12)***

**DDP DEMOGRAPHICS DATA FETCH**
    ***[GITHUB | DMP Repository State: 13](#github-dmp-repository-state-13)***


## 6. Cancer Type Case Lists and Supp Date Added Clinical Updates
**********************************************************************
**Cancer Type Case Lists and Supp Date Added Clinical Updates**
    *MSKIMPACT*
        ***[GITHUB | DMP Repository State: 14](#github-dmp-repository-state-14)***
        ***[GITHUB | DMP Repository State: 15](#github-dmp-repository-state-15)***
    *HEMEPACT*
        ***[GITHUB | DMP Repository State: 16](#github-dmp-repository-state-16)***
        ***[GITHUB | DMP Repository State: 17](#github-dmp-repository-state-17)***
    *ARCHER*
        ***[GITHUB | DMP Repository State: 18](#github-dmp-repository-state-18)***
        ***[GITHUB | DMP Repository State: 19](#github-dmp-repository-state-19)***
    *RAINDANCE*
        ***[GITHUB | DMP Repository State: 20](#github-dmp-repository-state-20)***
        ***[GITHUB | DMP Repository State: 21](#github-dmp-repository-state-21)***
    *ACCESS*
        ***[GITHUB | DMP Repository State: 22](#github-dmp-repository-state-22)***
        ***[GITHUB | DMP Repository State: 23](#github-dmp-repository-state-23)***


## 7. Additional Processing Before Updating REDCap Projects
**********************************************************************
*MSKIMPACT-linked ARCHER fusion events merge*
    ***[GITHUB | DMP Repository State: 24](#github-dmp-repository-state-24)***
*HEMEPACT-linked ARCHER fusion events merge*
    ***[GITHUB | DMP Repository State: 25](#github-dmp-repository-state-25)***

## 8. Import Projects Into REDCap
**********************************************************************
    *MSKIMPACT*
        **CRDB**
        **DARWIN CAISIS**
        **DDP**
        **CVR AND CLINICAL SUPP DATE ADDED**
    *HEMEPACT*
        **DDP**
        **CVR AND CLINICAL SUPP DATE ADDED**
    *ARCHER*
        **DDP**
        **CVR AND CLINICAL SUPP DATE ADDED**
    *RAINDANCE*
        **DDP**
        **CVR AND CLINICAL SUPP DATE ADDED**
    *ACCESS*
        **DDP**
        **CVR AND CLINICAL SUPP DATE ADDED**


## 9. Remove Raw Clinical and Timeline Files
**********************************************************************
*Remove Raw Clinical and Timeline Files*
    ***[GITHUB | DMP Repository State: 26](#github-dmp-repository-state-26)***

## 10. Clinical and Timeline cBioPortal Staging Files REDCap Exports
**********************************************************************
**Clinical and Timeline cBioPortal Staging Files REDCap Exports**
    *MSKIMPACT*
        ***[GITHUB | DMP Repository State: 27](#github-dmp-repository-state-27)***
    *HEMEPACT*
        ***[GITHUB | DMP Repository State: 28](#github-dmp-repository-state-28)***
    *RAINDANCE*
        ***[GITHUB | DMP Repository State: 29](#github-dmp-repository-state-29)***
    *ARCHER*
        ***[GITHUB | DMP Repository State: 30](#github-dmp-repository-state-30)***
    *ACCESS*
        ***[GITHUB | DMP Repository State: 31](#github-dmp-repository-state-31)***


## 11. Unlinked ARCHER  Data Processing
**********************************************************************
*Unlinked ARCHER  Data Processing*
    ***[GITHUB | DMP Repository State: 32](#github-dmp-repository-state-32)***
    ***[GITHUB | DMP Repository State: 33](#github-dmp-repository-state-33)***

## 12. MIXEDPACT and MSKSOLIDHEME Updates
**********************************************************************
    *MIXEDPACT*
    *MSKSOLIDHEME*
    ***[GITHUB | DMP Repository State: 34](#github-dmp-repository-state-34)***
    ***[GITHUB | DMP Repository State: 35](#github-dmp-repository-state-35)***

## 13. Affiliate Cohort Updates
**********************************************************************
**INSTITUTE-SPECIFIC SUBSETS**
    *KINGSCOUNTY*
        ***[GITHUB | DMP Repository State: 36](#github-dmp-repository-state-36)***
    *LEHIGHVALLEY*
        ***[GITHUB | DMP Repository State: 37](#github-dmp-repository-state-37)***
    *QUEENSCANCERCENTER*
        ***[GITHUB | DMP Repository State: 38](#github-dmp-repository-state-38)***
    *MIAMICANCERINSTITUTE*
        ***[GITHUB | DMP Repository State: 39](#github-dmp-repository-state-39)***
    *HARTFORDHEALTHCARE*
        ***[GITHUB | DMP Repository State: 40](#github-dmp-repository-state-40)***
    *RALPHLAUREN*
        ***[GITHUB | DMP Repository State: 41](#github-dmp-repository-state-41)***
    *RIKENGENESISJAPAN*
        ***[GITHUB | DMP Repository State: 42](#github-dmp-repository-state-42)***
**PEDIATRIC COHORT SUBSET**
    ***[GITHUB | DMP Repository State: 43](#github-dmp-repository-state-43)***
**SCLC COHORT SUBSET**
    ***[GITHUB | DMP Repository State: 44](#github-dmp-repository-state-44)***
**LYMPHOMASUPERCOHORT SUBSET**
    ***[GITHUB | DMP Repository State: 45](#github-dmp-repository-state-45)***

## 14. Push Updates to GIT Repository
**********************************************************************

## 15. Email Reports
**********************************************************************
