SCRIPT_START
{
NOP

LVAR_INT scplayer region loaded curregion curfile temporary node file filememory count number offset object curobject

LVAR_INT visualScript nodeCount vehNodeCount naviCount linkCount ix iy iz modified manipulationScript selecttype
LVAR_FLOAT x y z angle dist
curobject = -1
modified = 0

//Lists
LVAR_INT loadedregions list

CREATE_LIST DATATYPE_INT loadedregions

GET_LAST_CREATED_CUSTOM_SCRIPT number

GET_PLAYER_CHAR 0 scplayer
STREAM_CUSTOM_SCRIPT_FROM_LABEL visualEditor 0
GET_LAST_CREATED_CUSTOM_SCRIPT visualScript

STREAM_CUSTOM_SCRIPT_FROM_LABEL manipulationEditor 0
GET_LAST_CREATED_CUSTOM_SCRIPT manipulationScript

SET_SCRIPT_VAR visualScript 11 number //Pointer pra ca
SET_SCRIPT_VAR visualScript 9 manipulationScript //Pointer pro Manipulator
SET_SCRIPT_VAR manipulationScript 27 visualscript
SET_SCRIPT_VAR manipulationScript 5 number

GET_LABEL_POINTER modelIds temporary

IF NOT READ_INT_FROM_INI_FILE "cleo\VisualPathEditor.ini" "MODELS" "pednode" number
    PRINT_STRING_NOW "~r~VisualPathEditor.ini nao encontrado" 3000
    TERMINATE_THIS_CUSTOM_SCRIPT
ENDIF

READ_INT_FROM_INI_FILE "cleo\VisualPathEditor.ini" "MODELS" "pednode" number
WRITE_STRUCT_OFFSET temporary 0 4(number)

READ_INT_FROM_INI_FILE "cleo\VisualPathEditor.ini" "MODELS" "vehnode" number
WRITE_STRUCT_OFFSET temporary 4 4 (number)

READ_INT_FROM_INI_FILE "cleo\VisualPathEditor.ini" "MODELS" "selectnode" number
WRITE_STRUCT_OFFSET temporary 8 4 (number)

READ_INT_FROM_INI_FILE "cleo\VisualPathEditor.ini" "MODELS" "navinode" number
WRITE_STRUCT_OFFSET temporary 12 4 (number)

main:
    WAIT 0

    GET_CHAR_COORDINATES scplayer x y z
    CLEO_CALL getRegion 0 (x, y) temporary

    IF NOT temporary = region
    AND loaded = TRUE
        CLEO_CALL getRegion 0 (x, y) (region)
        GOSUB updateCurfile
    ENDIF

    IF loaded = TRUE
        IF IS_KEY_JUST_PRESSED VK_KEY_O
        OR IS_ANY_FIRE_BUTTON_PRESSED PAD1
            SET_LOCAL_VAR_BIT_LVAR modified curfile
            GOSUB getClosestObject
        ENDIF

        GOSUB loadZones
    ENDIF

    GET_LABEL_POINTER functionz count
    READ_MEMORY count 4 0 (count)
    IF NOT count = 0
        SWITCH count
            CASE 1  //Save
                IF loaded = TRUE
                    GOSUB saveRegions

                    WAIT 250 //Se não der wait o jogo não exclui os objects caso tenham muitos
                    loaded = FALSE
                    GOSUB deleteObjects
                    GOSUB unloadFiles
                ENDIF
            BREAK
            CASE 2  //Load
                IF loaded = FALSE
                    GOSUB desselect
                    GOSUB loadZones

                    GET_CHAR_COORDINATES scplayer x y z
                    CLEO_CALL getRegion 0 (x, y) (region)
                    GOSUB loadRegions
                    loaded = TRUE
                    SET_SCRIPT_VAR manipulationScript 21 loaded
                ENDIF
            BREAK
            CASE 3  //Unload (sem salvar)
                IF loaded = TRUE
                    loaded = FALSE
                    GOSUB deleteObjects
                    GOSUB unloadFiles
                ENDIF
            BREAK
            CASE 4  //Atualiza os links dos objects para salvamento
                IF loaded = TRUE
                    GOSUB updateLinkObjects
                ENDIF
            BREAK
            CASE 5  //Nova node de veículo
                GET_CHAR_COORDINATES scplayer x y z
                CLEO_CALL createNewNode 0 (0, loadedregions, x, y, z) //0 = car, 2 = boat
                GOSUB updateCounts
            BREAK
            CASE 6  //Deleta a node atual
                IF DOES_OBJECT_EXIST object
                AND DOES_OBJECT_EXIST curobject
                    CLEO_CALL removeCurrentNode 0 (object, curfile, selecttype, loadedregions)

                    //Já que removemos uma node, obrigatoriamente temos que atualizar todas as nodes ao redor
                    GET_SCRIPT_VAR manipulationScript 6 (temporary)
                    REPEAT 9 count
                        //A var do manipulador dita quais areas estão carregadas, sem isso o jogo tentara carregar áreas que não existem
                        IF IS_LOCAL_VAR_BIT_SET_LVAR temporary count
                            SET_LOCAL_VAR_BIT_LVAR modified count
                        ENDIF
                    ENDREPEAT

                    DELETE_OBJECT curobject
                    curobject = -1

                    SET_SCRIPT_VAR visualScript 3 (0) //Diz q não tem object
                    SET_SCRIPT_VAR visualScript 13 (0) //Diz q não tem curobject
                    SET_SCRIPT_VAR manipulationScript 14 (0)
                    SET_SCRIPT_VAR manipulationScript 22 (0) //NaviArea
                    SET_SCRIPT_VAR manipulationScript 23 (0) //NaviNode
                    GOSUB updateCounts
                ENDIF
            BREAK
            CASE 7  //Atualiza o node ligado ao NaviNode
                GOSUB updateNaviObjects
            BREAK
            CASE 8  //Cria uma nova node de ped
                GET_CHAR_COORDINATES scplayer x y z
                CLEO_CALL createNewNode 0 (1, loadedregions, x, y, z)
                GOSUB updateCounts
            BREAK
            CASE 9  //Cria uma nova navi node
                GET_CHAR_COORDINATES scplayer x y z
                CLEO_CALL createNewNode 0 (3, loadedregions, x, y, z)
                GOSUB updateCounts
            BREAK
            CASE 10 //Apenas desseleciona qualquer nod
                GOSUB desselect
            BREAK
        ENDSWITCH

        GET_LABEL_POINTER functionz count
        WRITE_MEMORY count 4 (0) 0
    ENDIF
GOTO main

//////////////////////////////////////////////////FUNCTIONALITY///////////////////////////////////////////////////////
    loadZones:
        GET_CHAR_COORDINATES scplayer x y z
        CLEO_CALL getRegion 0 (x, y) region

        SET_SCRIPT_VAR visualScript 0 region //Region
        SET_SCRIPT_VAR manipulationScript 6 0
        number = region
        
        temporary = 0
        MOD number 8 ix
        IF NOT ix = 0
            IF NOT number >= 56
                //TopLeft
                iy = 0
                SET_LOCAL_VAR_BIT_LVAR temporary iy
            ENDIF

            //Left
            iy = 3
            SET_LOCAL_VAR_BIT_LVAR temporary iy

            IF NOT number <= 7
                //BottomLeft
                iy = 5
                SET_LOCAL_VAR_BIT_LVAR temporary iy
            ENDIF
        ENDIF

        number += 1
        MOD number 8 ix
        IF NOT ix = 0
            IF NOT number >= 56
                //TopRight
                iy = 2
                SET_LOCAL_VAR_BIT_LVAR temporary iy
            ENDIF

            //Right
            iy = 4
            SET_LOCAL_VAR_BIT_LVAR temporary iy

            IF NOT number <= 7
                //BottomRight
                iy = 7
                SET_LOCAL_VAR_BIT_LVAR temporary iy
            ENDIF
        ENDIF
        number -= 1

        IF NOT number <= 7
            //Bottom
            iy = 6
            SET_LOCAL_VAR_BIT_LVAR temporary iy
        ENDIF

        IF NOT number >= 56
            //Top
            iy = 1
        SET_LOCAL_VAR_BIT_LVAR temporary iy
        ENDIF

        SET_SCRIPT_VAR manipulationScript 6 temporary
        SET_SCRIPT_VAR manipulationScript 15 (loadedregions)
    RETURN

    loadRegions:
        GET_SCRIPT_VAR manipulationScript 6 curregion
        SET_SCRIPT_VAR manipulationScript 7 1 //Loading
        WAIT 100

        RESET_LIST loadedregions

        //Cria a lista de nodes inter-regiões
        GET_LABEL_POINTER interregion iy
        CREATE_LIST DATATYPE_INT temporary
        WRITE_MEMORY iy 4 (temporary) 0 //Interregion List

        curfile = 0
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region += 7
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region -= 7
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 1
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region += 8
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region -= 8
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 2
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region += 9
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region -= 9
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 3
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region -= 1
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region += 1
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 4
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region += 1
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region -= 1
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 5
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region -= 9
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region += 9
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 6
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region -= 8
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region += 8
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 7
        IF IS_LOCAL_VAR_BIT_SET_LVAR curregion curfile
            region -= 7
            GOSUB loadFile
            GOSUB createObjects
            GOSUB createLinks
            LIST_ADD loadedregions region
            region += 7
        ELSE
            LIST_ADD loadedregions -1
        ENDIF

        curfile = 8
        GOSUB loadFile
        GOSUB createObjects
        GOSUB createLinks
        LIST_ADD loadedregions region

        SET_SCRIPT_VAR manipulationScript 7 0 //Loading
        SET_SCRIPT_VAR manipulationScript 8 region //EditingRegion
    RETURN

    loadFile:
        WAIT 0 //Só pra evitar freeze

        GET_LABEL_POINTER stringLabel (temporary)
        STRING_FORMAT (temporary) "cleo\gta3img\nodes%i.dat" region

        IF DOES_FILE_EXIST $temporary
            OPEN_FILE $temporary 0x6272 file
        ELSE
            PRINT_STRING_NOW "File does not exist" 3000
            RETURN
        ENDIF

        //Aloca memória suficiente pra armazenar o arquivo
        GET_FILE_SIZE file temporary
        CLEO_CALL getListPointer 0 (3 curfile) (list) //FileList
        ALLOCATE_MEMORY temporary filememory //Escreve nos endereços de memória para arquivos

        //Copia todo o arquivo pra memória e fecha
        iz = 0
        WHILE iz < temporary
            FILE_SEEK file iz 0
            READ_FROM_FILE file 4 ix
            WRITE_STRUCT_OFFSET filememory iz 4 ix
            iz += 4
        ENDWHILE

        WRITE_MEMORY list 4 filememory 0
        CLOSE_FILE file

        GOSUB updateCounts
        PRINT_FORMATTED_NOW "Region: %i, Nodes: %i, VehNodes: %i, Navis: %i, Links: %i" 3000 region nodeCount vehNodeCount naviCount linkCount
    RETURN

    unloadFiles:
        REPEAT 9 count
            CLEO_CALL getListPointer 0 (3 count) (temporary)
            READ_MEMORY temporary 4 0 (number)
            FREE_MEMORY number
        ENDREPEAT

        loaded = FALSE
        SET_SCRIPT_VAR manipulationScript 21 loaded
    RETURN

    updateCounts:
        CLEO_CALL getListPointer 0 (3 curfile) (list) //FileList
        READ_MEMORY list 4 0 filememory

        //Pega as variáveis do cabeçalho
        READ_STRUCT_OFFSET filememory 0 4 (nodeCount)
        READ_STRUCT_OFFSET filememory 4 4 (vehNodeCount)
        READ_STRUCT_OFFSET filememory 12 4 (naviCount)
        READ_STRUCT_OFFSET filememory 16 4 (linkCount)
    RETURN

    updateCurfile:
        GET_CHAR_COORDINATES scplayer x y z
        REPEAT 9 count
            GET_LIST_VALUE_BY_INDEX loadedregions count temporary
            IF temporary = region
                curfile = count
            ENDIF
        ENDREPEAT
        GOSUB updateCounts
        SET_SCRIPT_VAR manipulationScript 8 region
    RETURN

    createObjects:
        CLEO_CALL getListPointer 0 (0 curfile) (temporary) //ObjectList
        CREATE_LIST DATATYPE_INT list
        WRITE_MEMORY temporary 4 (list) 0

        CLEO_CALL getListPointer 0 (1 curfile) (temporary) //NodeList
        CREATE_LIST DATATYPE_INT list
        WRITE_MEMORY temporary 4 (list) 0

        CLEO_CALL getListPointer 0 (2 curfile) (temporary) //NaviList
        CREATE_LIST DATATYPE_INT list
        WRITE_MEMORY temporary 4 (list) 0

        //Carrega os modelos
        GET_LABEL_POINTER modelIds temporary
        READ_STRUCT_OFFSET temporary 0 4 (number)
        REQUEST_MODEL number

        READ_STRUCT_OFFSET temporary 4 4 (number)
        REQUEST_MODEL number

        READ_STRUCT_OFFSET temporary 12 4 (number)
        REQUEST_MODEL number

        LOAD_ALL_MODELS_NOW

        count = 0
        WHILE count < nodeCount
            CLEO_CALL getNodeOffset 0 (count) (offset)

            offset += 8

            ix = 0
            iy = 0
            iz = 0
            READ_STRUCT_OFFSET_MULTI filememory offset 3 2 (ix, iy, iz)

            CLEO_CALL convertToCoord 0 (ix) (x)
            CLEO_CALL convertToCoord 0 (iy) (y)
            CLEO_CALL convertToCoord 0 (iz) (z)
            
            GET_LABEL_POINTER modelIds temporary
            IF count >= vehNodeCount
                READ_STRUCT_OFFSET temporary 0 4 (number)
                CREATE_OBJECT_NO_SAVE number x y z TRUE FALSE object
            ELSE
                READ_STRUCT_OFFSET temporary 4 4 (number)
                CREATE_OBJECT_NO_SAVE number x y z TRUE FALSE object
            ENDIF

            SET_OBJECT_COLLISION object 0
            
            //Adiciona as listas
            CLEO_CALL getListPointer 0 (0 curfile) (temporary) //ObjectList
            READ_MEMORY temporary 4 0 list
            LIST_ADD list object

            CLEO_CALL getListPointer 0 (1 curfile) (temporary) //NodeList
            READ_MEMORY temporary 4 0 list
            LIST_ADD list object

            INIT_EXTENDED_OBJECT_VARS object VPEV 13

            offset += 8
            //Cabo as vars

            READ_STRUCT_OFFSET filememory offset 2 (temporary) //LinkID
            SET_EXTENDED_OBJECT_VAR object VPEV 1 temporary
            offset += 2

            READ_STRUCT_OFFSET filememory offset 2 (temporary) //AreaID
            SET_EXTENDED_OBJECT_VAR object VPEV 2 temporary
            offset += 2

            READ_STRUCT_OFFSET filememory offset 2 (temporary) //NodeID
            SET_EXTENDED_OBJECT_VAR object VPEV 3 temporary
            offset += 2

            READ_STRUCT_OFFSET filememory offset 1 (temporary) //Path Width
            SET_EXTENDED_OBJECT_VAR object VPEV 4 temporary
            offset += 1

            READ_STRUCT_OFFSET filememory offset 1 (temporary) //FloodFill
            SET_EXTENDED_OBJECT_VAR object VPEV 5 temporary
            offset += 1

            READ_STRUCT_OFFSET filememory offset 4 (temporary) //Flags
            SET_EXTENDED_OBJECT_VAR object VPEV 6 temporary

            //Number of Links
            number = 0
            CLEO_CALL getValueFromFlag 0 (temporary, 0, 4) (number)

            SET_EXTENDED_OBJECT_VAR object VPEV 8 number //Links

            CREATE_LIST DATATYPE_INT temporary
            SET_EXTENDED_OBJECT_VAR object VPEV 9 temporary //LinkAreaList
            
            CREATE_LIST DATATYPE_INT temporary
            SET_EXTENDED_OBJECT_VAR object VPEV 10 temporary //LinkNodeList

            CREATE_LIST DATATYPE_INT temporary
            SET_EXTENDED_OBJECT_VAR object VPEV 11 temporary //LinkNaviList

            CREATE_LIST DATATYPE_INT temporary
            SET_EXTENDED_OBJECT_VAR object VPEV 12 temporary //LinkLengthList

            CREATE_LIST DATATYPE_INT temporary
            SET_EXTENDED_OBJECT_VAR object VPEV 13 temporary //LinkObjectList

            object = 0
            count += 1
        ENDWHILE

        count = 0
        WHILE count < naviCount
            CLEO_CALL getNaviOffset 0 (count, nodeCount) (offset)

            ix = 0
            iy = 0
            READ_STRUCT_OFFSET_MULTI filememory offset 2 2 (ix, iy)

            CLEO_CALL convertToCoord 0 (ix) (x)
            CLEO_CALL convertToCoord 0 (iy) (y)

            offset += 8

            ix = 0
            iy = 0
            READ_STRUCT_OFFSET_MULTI filememory offset 2 1 (ix, iy)

            IF ix > 128
                ix -= 256
            ENDIF

            IF iy > 128
                iy -= 256
            ENDIF

            GET_GROUND_Z_FOR_3D_COORD x y 900.0 z
            GET_WATER_HEIGHT_AT_COORDS x y TRUE angle

            IF z >= angle
                CREATE_OBJECT_NO_SAVE 1318 x y z TRUE FALSE object
            ELSE
                CREATE_OBJECT_NO_SAVE 1318 x y angle TRUE FALSE object
            ENDIF

            SET_OBJECT_COLLISION object 0

            //Adiciona as listas
            CLEO_CALL getListPointer 0 (0 curfile) (temporary) //ObjectList
            READ_MEMORY temporary 4 0 list
            LIST_ADD list object

            CLEO_CALL getListPointer 0 (2 curfile) (temporary) //NaviList
            READ_MEMORY temporary 4 0 list
            LIST_ADD list object

            CLEO_CALL getRadianAngle 0 (ix, iy) (angle)
            SET_OBJECT_ROTATION object 90.0 0.0 angle

            INIT_EXTENDED_OBJECT_VARS object VPEV 7
            
            SET_EXTENDED_OBJECT_VAR object VPEV 3 ix
            SET_EXTENDED_OBJECT_VAR object VPEV 4 iy

            offset -= 4

            READ_STRUCT_OFFSET filememory offset 2 temporary //AreaID
            SET_EXTENDED_OBJECT_VAR object VPEV 1 temporary
            offset += 2

            READ_STRUCT_OFFSET filememory offset 2 iz //NodeID
            SET_EXTENDED_OBJECT_VAR object VPEV 2 iz
            offset += 4

            READ_STRUCT_OFFSET filememory offset 4 iy //Flags
            SET_EXTENDED_OBJECT_VAR object VPEV 5 iy

            CLEO_CALL getListPointer 0 (1 curfile) (temporary) //Nodelist
            READ_MEMORY temporary 4 0 list

            //Path Width
            CLEO_CALL getValueFromFlag 0 (iy, 0, 8) (temporary)
            SET_EXTENDED_OBJECT_VAR object VPEV 6 temporary

            SET_EXTENDED_OBJECT_VAR object VPEV 7 count //NaviID

            GET_LIST_VALUE_BY_INDEX list iz (node)
            SET_EXTENDED_OBJECT_VAR node VPEV 7 object //Da pro node um pointer pra esse Navi

            count += 1
        ENDWHILE

        MARK_MODEL_AS_NO_LONGER_NEEDED 1880
        MARK_MODEL_AS_NO_LONGER_NEEDED 1877
        MARK_MODEL_AS_NO_LONGER_NEEDED 1318
    RETURN

    createLinks:
        WAIT 0
        count = 0
        number = 0
        WHILE count < linkCount
            CLEO_CALL getLinkOffset 0 (count nodeCount naviCount) (offset)
            
            CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
            READ_MEMORY list 4 0 list
            GET_LIST_VALUE_BY_INDEX list number (node)

            IF DOES_OBJECT_EXIST node
                iz = 0
                temporary = 0
                READ_STRUCT_OFFSET_MULTI filememory offset 2 2 (iz, temporary)

                //Adiciona a lista de nodes entre regiões
                GET_EXTENDED_OBJECT_VAR node VPEV 2 iy //AreaID
                IF NOT iy = iz
                    GET_LABEL_POINTER interregion iy
                    READ_MEMORY iy 4 0 (iy) //Interregion List
                    LIST_ADD iy node
                ENDIF 

                GET_EXTENDED_OBJECT_VAR node VPEV 9 ix //LinkAreaList
                GET_EXTENDED_OBJECT_VAR node VPEV 10 iy //LinkNodeList

                LIST_ADD ix iz          //Area
                LIST_ADD iy temporary   //Node

                GET_LIST_SIZE ix temporary
                GET_EXTENDED_OBJECT_VAR node VPEV 8 iy //LinkAreaList

                IF temporary = iy //List as big as LinkSize
                    number += 1
                ENDIF

                iz = 0
                CLEO_CALL getNaviLinkOffset 0 (count nodeCount naviCount linkCount) (offset)
                READ_STRUCT_OFFSET filememory offset 2 (iz)

                temporary = 0
                CLEO_CALL getLinkLengthOffset 0 (count nodeCount naviCount linkCount) (offset)
                READ_STRUCT_OFFSET filememory offset 1 (temporary)

                GET_EXTENDED_OBJECT_VAR node VPEV 11 ix //LinkNaviList
                GET_EXTENDED_OBJECT_VAR node VPEV 12 iy //LinkLengthList

                LIST_ADD ix iz
                LIST_ADD iy temporary

                GET_EXTENDED_OBJECT_VAR node VPEV 13 ix //LinkObject
                READ_STRUCT_OFFSET_MULTI filememory offset 2 2 (iz, temporary)

                CLEO_CALL getNodebyID 0 (loadedregions, iz, temporary) (temporary)
                LIST_ADD ix temporary
            ENDIF

            count += 1
        ENDWHILE
    RETURN

    deleteObjects:
        GET_LABEL_POINTER lists temporary //ObjectList
        SET_SCRIPT_VAR manipulationScript 7 1 //Loading
        WAIT 100

        REPEAT 9 ix
            GET_LIST_VALUE_BY_INDEX loadedregions ix temporary
            IF NOT temporary = -1
                CLEO_CALL getListPointer 0 (0 ix) (list) //ObjectList
                READ_MEMORY list 4 0 list
                GET_LIST_SIZE list number
                count = 0
                WHILE count < number
                    GET_LIST_VALUE_BY_INDEX list count (object)
                    IF DOES_OBJECT_EXIST object
                        DELETE_OBJECT object
                    ENDIF
                    count += 1
                ENDWHILE

                SET_SCRIPT_VAR visualScript 8 (0) //Diz q não tem object
                SET_SCRIPT_VAR manipulationScript 14 (0)

                IF DOES_OBJECT_EXIST curobject
                    DELETE_OBJECT curobject
                ENDIF

                DELETE_LIST list

                CLEO_CALL getListPointer 0 (0 ix) (list) //NodeList
                READ_MEMORY list 4 0 list
                DELETE_LIST list

                CLEO_CALL getListPointer 0 (0 ix) (list) //NaviList
                READ_MEMORY list 4 0 list
                DELETE_LIST list
            ENDIF
        ENDREPEAT

        GET_LABEL_POINTER interregion iy
        READ_MEMORY iy 4 0 (temporary) //Interregion List
        DELETE_LIST temporary

        SET_SCRIPT_VAR manipulationScript 7 0 //Loading
    RETURN

    getClosestObject:
        CLEO_CALL getListPointer 0 (0 curfile) (list) //ObjectList
        READ_MEMORY list 4 0 list
        GET_LIST_SIZE list number

        IF DOES_OBJECT_EXIST curobject
            DELETE_OBJECT curobject
            curobject = -1
        ENDIF

        count = 0
        WHILE count < number
            GET_LIST_VALUE_BY_INDEX list count (object)
            IF DOES_OBJECT_EXIST object
                IF LOCATE_CHAR_DISTANCE_TO_OBJECT scplayer object 2.0
                    curobject = object
                    GET_OBJECT_MODEL curobject offset
                    IF offset = 1318
                        selecttype = 2
                    ELSE
                        selecttype = 1
                    ENDIF

                    SET_SCRIPT_VAR manipulationScript 26 selecttype
                    SET_SCRIPT_VAR visualScript 10 selecttype //SelectType

                    GET_OBJECT_COORDINATES curobject x y z

                    GET_LABEL_POINTER modelIds temporary
                    READ_STRUCT_OFFSET temporary 8 4 (ix)
                    REQUEST_MODEL ix
                    LOAD_ALL_MODELS_NOW

                    CREATE_OBJECT_NO_SAVE ix x y z FALSE FALSE curobject

                    SET_OBJECT_COLLISION curobject 0

                    INIT_EXTENDED_OBJECT_VARS curobject VPEV 1
                    SET_EXTENDED_OBJECT_VAR curobject VPEV 1 object
                    SET_SCRIPT_VAR visualScript 3 (object) //Manda o object inteiro mesmo e foda-se

                    //Manda o object e curobject pro script de manipulação
                    SET_SCRIPT_VAR manipulationScript 14 (object)  
                    SET_SCRIPT_VAR manipulationScript 21 (curobject)
                    SET_SCRIPT_VAR visualScript 13 (curobject) //Manda o curobject

                    IF selecttype = 1
                        //Selecionando Nodes
                        GET_EXTENDED_OBJECT_VAR object VPEV 6 (temporary)
                        SET_SCRIPT_VAR visualScript 1 (temporary) //manda as flags pro outro script visual

                        GET_EXTENDED_OBJECT_VAR object VPEV 8 (temporary)
                        SET_SCRIPT_VAR visualScript 4 (temporary) //Links
                        SET_SCRIPT_VAR manipulationScript 9 (temporary)

                        GET_EXTENDED_OBJECT_VAR object VPEV 9 (temporary)
                        SET_SCRIPT_VAR visualScript 5 (temporary) //a list com as Areas dos Links

                        GET_EXTENDED_OBJECT_VAR object VPEV 10 (temporary)
                        SET_SCRIPT_VAR visualScript 6 (temporary) //a list com as Nodes dos Links

                        GET_EXTENDED_OBJECT_VAR object VPEV 11 (temporary)
                        SET_SCRIPT_VAR visualScript 7 (temporary) //a list com as Navis dos Links

                        GET_EXTENDED_OBJECT_VAR object VPEV 12 (temporary)
                        SET_SCRIPT_VAR visualScript 8 (temporary) //a list com as Lenghts dos Links

                        GET_EXTENDED_OBJECT_VAR object VPEV 3 (temporary)
                        IF temporary >= vehNodeCount
                            SET_SCRIPT_VAR visualScript 2 (0) //seta IsVehicle pra 0
                        ELSE
                            SET_SCRIPT_VAR visualScript 2 (1) //é veículo, então seta IsVehicle pra 1
                        ENDIF

                        //Ativa a visualização do seu NaviNode
                        GET_EXTENDED_OBJECT_VAR object VPEV 7 (temporary)
                        SET_SCRIPT_VAR manipulationScript 22 (temporary)
                    ELSE
                        //Selecionando Navis
                        GET_EXTENDED_OBJECT_VAR object VPEV 1 (temporary)
                        SET_SCRIPT_VAR visualScript 4 (temporary) //AreaID

                        GET_EXTENDED_OBJECT_VAR object VPEV 2 (temporary)
                        SET_SCRIPT_VAR visualScript 5 (temporary) //NodeID

                        GET_EXTENDED_OBJECT_VAR object VPEV 5 (temporary)
                        SET_SCRIPT_VAR visualScript 1 (temporary) //Flags dos Navis

                        GET_EXTENDED_OBJECT_VAR object VPEV 6 (temporary)
                        SET_SCRIPT_VAR visualScript 6 (temporary) //Pathwidth
                    ENDIF

                    SET_OBJECT_SCALE curobject 1.2

                    SET_LOCAL_VAR_BIT_LVAR modified curfile

                    RETURN
                ENDIF
            ENDIF
            count += 1
        ENDWHILE

        IF NOT DOES_OBJECT_EXIST curobject
            SET_SCRIPT_VAR visualScript 3 (0) //Diz q não tem object
            SET_SCRIPT_VAR visualScript 13 (0) //Diz q não tem curobject
            SET_SCRIPT_VAR manipulationScript 14 (0)
            SET_SCRIPT_VAR manipulationScript 22 (0) //NaviArea
            SET_SCRIPT_VAR manipulationScript 23 (0) //NaviNode
        ENDIF
    RETURN

    updateLinkObjects:
        CLEO_CALL getListPointer 0 (0 curfile) (list) //ObjectList
        READ_MEMORY list 4 0 list
        GET_LIST_SIZE list number

        count = 0
        WHILE count < number
            GET_LIST_VALUE_BY_INDEX list count (node)
            IF DOES_OBJECT_EXIST node
                IF LOCATE_CHAR_DISTANCE_TO_OBJECT scplayer node 1.2
                    GET_OBJECT_MODEL node offset
                    IF offset = 1318
                        selecttype = 2
                    ELSE
                        selecttype = 1
                    ENDIF

                    GET_SCRIPT_VAR visualScript 20 (offset)
                    offset -= 1

                    IF selecttype = 1
                        //Selecionando Nodes
                        GET_EXTENDED_OBJECT_VAR object VPEV 9 (temporary) //LinkArea
                        GET_EXTENDED_OBJECT_VAR node VPEV 2 (number)
                        CLEO_CALL setListValue 0 (temporary, offset, number) (temporary)
                        SET_EXTENDED_OBJECT_VAR object VPEV 9 (temporary)

                        GET_EXTENDED_OBJECT_VAR object VPEV 10 (temporary) //LinkNode
                        GET_EXTENDED_OBJECT_VAR node VPEV 3 (number)
                        CLEO_CALL setListValue 0 (temporary, offset, number) (temporary)
                        SET_EXTENDED_OBJECT_VAR object VPEV 10 (temporary)

                        GET_OBJECT_COORDINATES curobject x y dist
                        GET_OBJECT_COORDINATES node z angle dist

                        GET_DISTANCE_BETWEEN_COORDS_2D x y z angle dist

                        ix =# dist

                        GET_EXTENDED_OBJECT_VAR object VPEV 12 (temporary) //LinkLength
                        CLEO_CALL setListValue 0 (temporary, offset, ix) (temporary)
                        SET_EXTENDED_OBJECT_VAR object VPEV 12 (temporary)
                    ELSE
                        //Selecionando Navis
                        GET_EXTENDED_OBJECT_VAR object VPEV 11 (temporary) //NaviLink

                        iy = 0
                        GET_EXTENDED_OBJECT_VAR node VPEV 7 (ix) //NaviID
                        CLEO_CALL setFlagFromValue 0 (iy, 0, 10, ix) (iy)
                        
                        GET_EXTENDED_OBJECT_VAR node VPEV 1 (ix) //AreaID
                        CLEO_CALL setFlagFromValue 0 (iy, 10, 6, ix) (iy)

                        CLEO_CALL setListValue 0 (temporary, offset, iy) (temporary)

                        SET_EXTENDED_OBJECT_VAR object VPEV 11 (temporary)
                    ENDIF
                    RETURN
                ENDIF
            ENDIF
            count += 1
        ENDWHILE
    RETURN

    updateNaviObjects:
        CLEO_CALL getListPointer 0 (0 curfile) (list) //ObjectList
        READ_MEMORY list 4 0 list
        GET_LIST_SIZE list number

        count = 0
        WHILE count < number
            GET_LIST_VALUE_BY_INDEX list count (node)
            IF DOES_OBJECT_EXIST node
                IF LOCATE_CHAR_DISTANCE_TO_OBJECT scplayer node 2.0
                    GET_OBJECT_MODEL node offset
                    IF NOT offset = 1318
                        GET_EXTENDED_OBJECT_VAR node VPEV 2 ix //AreaID
                        GET_EXTENDED_OBJECT_VAR node VPEV 3 iy //NodeID

                        SET_EXTENDED_OBJECT_VAR object VPEV 1 ix //AreaID
                        SET_EXTENDED_OBJECT_VAR object VPEV 2 iy //NodeID
                    ENDIF
                ENDIF
            ENDIF
            count += 1
        ENDWHILE
    RETURN

    saveRegions:
        GET_SCRIPT_VAR manipulationScript 6 curregion
        SET_SCRIPT_VAR manipulationScript 7 1 //Loading
        WAIT 1

        curfile = 0
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region += 7
            GOSUB writeFile
            region -= 7
        ENDIF

        curfile = 1
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region += 8
            GOSUB writeFile
            region -= 8
        ENDIF

        curfile = 2
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region += 9
            GOSUB writeFile
            region -= 9
        ENDIF

        curfile = 3
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region -= 1
            GOSUB writeFile
            region += 1
        ENDIF

        curfile = 4
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region += 1
            GOSUB writeFile
            region -= 1
        ENDIF

        curfile = 5
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region -= 9
            GOSUB writeFile
            region += 9
        ENDIF

        curfile = 6
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region -= 8
            GOSUB writeFile
            region += 8
        ENDIF

        curfile = 7
        IF IS_LOCAL_VAR_BIT_SET_LVAR modified curfile
            region -= 7
            GOSUB writeFile
            region += 7
        ENDIF

        curfile = 8
        GOSUB writeFile
        RESET_LIST loadedregions

        SET_SCRIPT_VAR manipulationScript 7 0 //Loading
        SET_SCRIPT_VAR manipulationScript 8 region //EditingRegion
    RETURN

    desselect:
        IF DOES_OBJECT_EXIST curobject
            DELETE_OBJECT curobject
            curobject = -1
        ENDIF
        
        SET_SCRIPT_VAR visualScript 3 (0) //Diz q não tem object
        SET_SCRIPT_VAR visualScript 13 (0) //Diz q não tem curobject
        SET_SCRIPT_VAR manipulationScript 14 (0)
        SET_SCRIPT_VAR manipulationScript 22 (0) //NaviArea
        SET_SCRIPT_VAR manipulationScript 23 (0) //NaviNode
    RETURN

    writeFile:
        GET_LABEL_POINTER stringLabel (temporary)

        GOSUB updateCounts

        STRING_FORMAT (temporary) "cleo\gta3img\compiled\nodes%i.dat" region
        OPEN_FILE $temporary 0x6277 file

        CLEO_CALL updateLinkIDs 0 (curfile) linkCount

        //Desseleciona qualquer node pra evitar crashes
        GOSUB desselect

        //Header
        number = nodeCount
        number -= vehNodeCount
        CLEO_CALL writeFileOffset 0 (file, 0, 4) (nodeCount)      //Node Count
        CLEO_CALL writeFileOffset 0 (file, 4, 4) (vehNodeCount)   //Vehicle Node Count
        CLEO_CALL writeFileOffset 0 (file, 8, 4) (number)           //Ped Node Count
        CLEO_CALL writeFileOffset 0 (file, 12, 4) (naviCount)     //Navi Node Count
        CLEO_CALL writeFileOffset 0 (file, 16, 4) (linkCount)     //Link Count

        //Sector 1
            count = 0
            WHILE count < nodeCount
                CLEO_CALL getNodeOffset 0 (count) (offset)

                CLEO_CALL writeFileOffset 0 (file, offset, 4) (27788808)
                offset += 4
                CLEO_CALL writeFileOffset 0 (file, offset, 4) (0)
                offset += 4

                x = 0.0
                y = 0.0
                z = 0.0

                //Pega o object da node
                CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
                READ_MEMORY list 4 0 list
                GET_LIST_VALUE_BY_INDEX list count (node)
                
                GET_OBJECT_COORDINATES node x y z
                
                //Coordenadas
                CLEO_CALL convertToSignedCoord 0 (x) (ix)
                CLEO_CALL convertToSignedCoord 0 (y) (iy)
                CLEO_CALL convertToSignedCoord 0 (z) (iz)
                iz -= 2

                CLEO_CALL writeFileOffset 0 (file, offset, 2) (ix)
                offset += 2
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (iy)
                offset += 2
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (iz)
                offset += 2

                CLEO_CALL writeFileOffset 0 (file, offset, 2) (0x7FFE) //Heuristic Cost, sempre vai ser isso
                offset += 2

                //IDs
                GET_EXTENDED_OBJECT_VAR node VPEV 1 (number) //LinkID
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 2

                GET_EXTENDED_OBJECT_VAR node VPEV 2 (number) //AreaID
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 2

                GET_EXTENDED_OBJECT_VAR node VPEV 3 (number) //NodeID
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 2

                //Misc
                GET_EXTENDED_OBJECT_VAR node VPEV 4 (number) //Path Width
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 1

                GET_EXTENDED_OBJECT_VAR node VPEV 5 (number) //Floodfill
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 1

                GET_EXTENDED_OBJECT_VAR node VPEV 6 (number) //Flags

                GET_EXTENDED_OBJECT_VAR node VPEV 8 (temporary) //LinkCount
                CLEO_CALL setFlagFromValue 0 (number, 0, 4, temporary) (number)
                temporary = 0

                CLEO_CALL writeFileOffset 0 (file, offset, 4) (number)
                count += 1
            ENDWHILE
        //

        //Sector 2
            count = 0
            WHILE count < naviCount
                CLEO_CALL getNaviOffset 0 (count, nodeCount) (offset)

                //Pega o object da navinode
                CLEO_CALL getListPointer 0 (2 curfile) (list) //NaviList
                READ_MEMORY list 4 0 list

                GET_LIST_VALUE_BY_INDEX list count (node)
                GET_OBJECT_COORDINATES node x y z

                //Coordenadas
                CLEO_CALL convertToSignedCoord 0 (x) (ix)
                CLEO_CALL convertToSignedCoord 0 (y) (iy)

                CLEO_CALL writeFileOffset 0 (file, offset, 2) (ix)
                offset += 2

                CLEO_CALL writeFileOffset 0 (file, offset, 2) (iy)
                offset += 2

                //AreaID e NodeID conectados
                GET_EXTENDED_OBJECT_VAR node VPEV 1 (number) //LinkID
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 2

                GET_EXTENDED_OBJECT_VAR node VPEV 2 (number) //NodeID
                CLEO_CALL writeFileOffset 0 (file, offset, 2) (number)
                offset += 2

                //Em caso do ID ser -1, abortar totalmente o salvamento
                IF number = -1
                    PRINT_FORMATTED_NOW "~r~Ha navis que nao estao conectadas a nenhum node!" 3000

                    GET_LABEL_POINTER functionz temporary
                    WRITE_MEMORY temporary 4 (0) 0

                    SET_SCRIPT_VAR manipulationScript 7 0 //Loading
                    GOTO main
                ENDIF

                //Direções
                GET_OBJECT_HEADING node angle
                CLEO_CALL getSignedAngle 0 (angle) (ix, iy)

                CLEO_CALL writeFileOffset 0 (file, offset, 1) (ix)
                offset += 1

                CLEO_CALL writeFileOffset 0 (file, offset, 1) (iy)
                offset += 1

                GET_EXTENDED_OBJECT_VAR node VPEV 3 number
                GET_EXTENDED_OBJECT_VAR node VPEV 4 temporary

                //Flags
                GET_EXTENDED_OBJECT_VAR node VPEV 5 (number) //Flags
                CLEO_CALL writeFileOffset 0 (file, offset, 4) (number)
                offset += 4

                count += 1
            ENDWHILE
        //

        //Sector 3&5&6
            count = 0
            WHILE count < naviCount
                CLEO_CALL getLinkOffset 0 (count, nodeCount, naviCount) (offset)

                //Pega o object da navinode
                CLEO_CALL getListPointer 0 (2 curfile) (list) //NaviList
                READ_MEMORY list 4 0 list

                count += 1
            ENDWHILE

            count = 0
            iz = 0
            WHILE count < nodeCount
                //Pega o object da node
                CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
                READ_MEMORY list 4 0 list
                GET_LIST_VALUE_BY_INDEX list count (node)

                GET_EXTENDED_OBJECT_VAR node VPEV 8 (temporary)

                number = 0
                WHILE number < temporary
                    //Sector 3
                    CLEO_CALL getLinkOffset 0 (iz, nodeCount, naviCount) (offset)

                    GET_EXTENDED_OBJECT_VAR node VPEV 9 (list) //Areas

                    ix = 0
                    GET_LIST_VALUE_BY_INDEX list number (ix)

                    CLEO_CALL writeFileOffset 0 (file, offset, 2) (ix)
                    GET_EXTENDED_OBJECT_VAR node VPEV 10 (list) //Nodes
                    offset += 2

                    iy = 0
                    GET_LIST_VALUE_BY_INDEX list number (iy)

                    //Em caso do ID ser -1, abortar totalmente o salvamento
                    IF number = -1
                        PRINT_FORMATTED_NOW "~r~Ha nodes com links invalidos!" 3000

                        GET_LABEL_POINTER functionz temporary
                        WRITE_MEMORY temporary 4 (0) 0

                        SET_SCRIPT_VAR manipulationScript 7 0 //Loading
                        GOTO main
                    ENDIF

                    CLEO_CALL writeFileOffset 0 (file, offset, 2) (iy)

                    //Sector 5
                    CLEO_CALL getNaviLinkOffset 0 (iz, nodeCount, naviCount, linkCount) (offset)
                    GET_EXTENDED_OBJECT_VAR node VPEV 11 (list)

                    ix = 0
                    GET_LIST_VALUE_BY_INDEX list number (ix)
                    CLEO_CALL writeFileOffset 0 (file, offset, 2) (ix)

                    //Sector 6
                    CLEO_CALL getLinkLengthOffset 0 (iz, nodeCount, naviCount, linkCount) (offset)
                    GET_EXTENDED_OBJECT_VAR node VPEV 12 (list)

                    ix = 0
                    GET_LIST_VALUE_BY_INDEX list number (ix)
                    
                    CLEO_CALL writeFileOffset 0 (file, offset, 1) (ix)

                    number += 1
                    iz += 1
                ENDWHILE

                count += 1
            ENDWHILE
        //

        //Sector 4
            offset = 0
            number = 4 * linkCount
            offset += number
            number = 28 * nodeCount
            offset += number
            number = 14 * naviCount
            offset += number
            offset += 20
            REPEAT 192 count
                CLEO_CALL writeFileOffset 0 (file, offset, 1) (0xFF)
                offset += 1
                CLEO_CALL writeFileOffset 0 (file, offset, 1) (0xFF)
                offset += 1
                CLEO_CALL writeFileOffset 0 (file, offset, 1) (0x00)
                offset += 1
                CLEO_CALL writeFileOffset 0 (file, offset, 1) (0x00)
                offset += 1
            ENDREPEAT
        //

        CLOSE_FILE file
    RETURN

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
SCRIPT_END 

/////////////////////////////////////////////////////VISUAL///////////////////////////////////////////////////////////
    {
    visualEditor:
    LVAR_INT region flags isvehicle object links linkarealist linknodelist
    LVAR_INT  linknavilist linklenghtlist manipulationScript selecttype mainscript scplayer curobject count value value2
    LVAR_INT math tab texture selected
    LVAR_FLOAT x y z fvalue
    LVAR_TEXT_LABEL txt

    GET_PLAYER_CHAR 0 scplayer

    LOAD_TEXTURE_DICTIONARY PATHTXD
    LOAD_SPRITE 1 booloff
    LOAD_SPRITE 2 boolon
    LOAD_SPRITE 3 typeped
    LOAD_SPRITE 4 typecar
    LOAD_SPRITE 5 typeboat

    tab = 1
    selected = 1

    menu:
        WAIT 0

        //Se o mod não estiver ativo nem roda o resto do código pra evitar crashs
        GET_SCRIPT_VAR manipulationScript 21 value
        IF value = FALSE
            GOTO menu
        ENDIF

        USE_TEXT_COMMANDS 1

        SET_SCRIPT_VAR manipulationScript 16 tab //Manda a tab pro Manipulador

        DRAW_STRING "LINKS" DRAW_EVENT_AFTER_DRAWING 91.0 6.0 0.45 0.85 TRUE FONT_MENU

        //Desenha o texto das tabs
        DRAW_STRING "CONFIGS" DRAW_EVENT_AFTER_DRAWING 10.0 7.0 0.45 0.85 TRUE FONT_MENU

        //Desenha a tab selecionada
        IF tab = 1
            DRAW_TEXTURE_PLUS 0 DRAW_EVENT_AFTER_FADE 111.0 14.0 96.0 20.0 0.0 2.0 TRUE 0 0 0 0 0 128
            GOSUB drawTab1
        ELSE
            DRAW_TEXTURE_PLUS 0 DRAW_EVENT_AFTER_FADE 39.0 14.0 96.0 20.0 0.0 2.0 TRUE 0 0 0 0 0 128
            GOSUB drawTab2
        ENDIF

        IF selecttype = 2
        AND tab = 2
            tab = 1
            selected = 1
        ENDIF

        IF selected = 0
            GET_SCRIPT_VAR manipulationScript 28 value
            IF IS_KEY_JUST_PRESSED VK_KEY_J
            OR IS_MOUSE_WHEEL_DOWN
                IF value = 0
                tab = 1
                ENDIF
            ENDIF

            IF IS_KEY_JUST_PRESSED VK_KEY_L
            OR IS_MOUSE_WHEEL_UP
                IF value = 0
                tab = 2
                ENDIF
            ENDIF

            IF tab = 1
                DRAW_RECT 39.0 14.0 72.0 20.0 128 24 24 128
            ELSE
                DRAW_RECT 111.0 14.0 72.0 20.0 128 24 24 128
            ENDIF
        ENDIF

        USE_TEXT_COMMANDS 0
    GOTO menu

    drawTab1:
        //Desenha a tabela inteira
        DRAW_TEXTURE_PLUS  0 DRAW_EVENT_AFTER_DRAWING 75.0 151.0 192.0 314.0 0.0 1.0 TRUE 0 0 20 20 20 200
        DRAW_STRING "Visual Path Editor by LightVelox" DRAW_EVENT_AFTER_DRAWING 56.0 288.0 0.15 0.325 TRUE FONT_MENU

        IF selecttype = 2 //NaviNodes
            DRAW_STRING "AreaID:" DRAW_EVENT_AFTER_DRAWING 9.0 34.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "NaviID:" DRAW_EVENT_AFTER_DRAWING 9.0 47.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "Path Width:" DRAW_EVENT_AFTER_DRAWING 9.0 60.0 0.3 0.65 TRUE FONT_MENU

            //Lanes
            DRAW_STRING "LANES" DRAW_EVENT_AFTER_DRAWING 55.0 78.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "Left:" DRAW_EVENT_AFTER_DRAWING 9.0 91.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "Right:" DRAW_EVENT_AFTER_DRAWING 9.0 104.0 0.3 0.65 TRUE FONT_MENU

            //Traffic Lights
            DRAW_STRING "Traffic Light Behaviour:" DRAW_EVENT_AFTER_DRAWING 9.0 124.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Traffic Light Direction:" DRAW_EVENT_AFTER_DRAWING 9.0 157.0 0.3 0.65 TRUE FONT_MENU

            //Train Boolean
            DRAW_STRING "Train Crossing:" DRAW_EVENT_AFTER_DRAWING 9.0 195.0 0.3 0.65 TRUE FONT_MENU

            IF NOT object = 0
            AND DOES_OBJECT_EXIST object
                //Desenha os valores
                GET_EXTENDED_OBJECT_VAR object VPEV 1 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 34.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 7 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 47.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 6 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 59.0 0.3 0.65 TRUE FONT_MENU

                //Lanes
                DRAW_STRING "Left:" DRAW_EVENT_AFTER_DRAWING 9.0 91.0 0.3 0.65 TRUE FONT_MENU
                DRAW_STRING "Right:" DRAW_EVENT_AFTER_DRAWING 9.0 104.0 0.3 0.65 TRUE FONT_MENU

                CLEO_CALL getValueFromFlag 0 (flags, 8,  3) value
                CLEO_CALL getValueFromFlag 0 (flags, 11, 3) value2

                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 125.0 91.0 0.3 0.65 TRUE FONT_MENU

                STRING_FORMAT txt "%i" value2
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 125.0 104.0 0.3 0.65 TRUE FONT_MENU

                //Traffic Light Behaviour
                CLEO_CALL getValueFromFlag 0 (flags, 16, 2) value
                SWITCH value
                    CASE 0
                        DRAW_STRING "DISABLED" DRAW_EVENT_AFTER_DRAWING 43.0 139.0 0.3 0.65 TRUE FONT_MENU
                    BREAK
                    CASE 1
                        DRAW_STRING "NORTH-SOUTH" DRAW_EVENT_AFTER_DRAWING 40.0 139.0 0.3 0.65 TRUE FONT_MENU
                    BREAK
                    CASE 2
                        DRAW_STRING "WEST-EAST" DRAW_EVENT_AFTER_DRAWING 45.0 139.0 0.3 0.65 TRUE FONT_MENU
                    BREAK
                ENDSWITCH

                math = 14
                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //Train Crossing
                    DRAW_STRING "Forward" DRAW_EVENT_AFTER_DRAWING 49.0 172.0 0.3 0.65 TRUE FONT_MENU
                ELSE
                    DRAW_STRING "Elsewhere" DRAW_EVENT_AFTER_DRAWING 43.0 172.0 0.3 0.65 TRUE FONT_MENU
                ENDIF


                //Train Boolean
                math = 18
                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //Train Crossing
                    GET_TEXTURE_FROM_SPRITE 2 texture
                ELSE
                    GET_TEXTURE_FROM_SPRITE 1 texture
                ENDIF

                DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD 128.0 201.0 10.0 10.0 0.0 1.0 TRUE 0 0 255 255 255 255

                //Desenha a caixa de seleção
                IF selected < 6
                    SWITCH selected
                        CASE 1
                            y = 97.0
                        BREAK
                        CASE 2
                            y = 110.0
                        BREAK
                        CASE 3
                            y = 145.0
                        BREAK
                        CASE 4
                            y = 178.0
                        BREAK
                        CASE 5
                            y = 201.0
                        BREAK
                    ENDSWITCH
                    DRAW_RECT 75.0 y 143.9 14.65 128 24 24 128
                ELSE
                    DRAW_RECT 75.0 242.0 145.9 50.35 128 24 24 128
                ENDIF

                GET_SCRIPT_VAR manipulationScript 28 value
                IF value = 0
                    //Controles
                    IF IS_KEY_JUST_PRESSED VK_KEY_I
                        selected -= 1
                        IF selected = -1
                            selected = 6
                        ENDIF
                    ENDIF

                    IF IS_KEY_JUST_PRESSED VK_KEY_K
                        selected += 1
                        IF selected = 7
                            selected = 0
                        ENDIF
                    ENDIF

                    IF IS_KEY_JUST_PRESSED VK_DELETE
                        GET_LABEL_POINTER functionz count
                        WRITE_MEMORY count 4 (6) 0
                    ENDIF

                    IF IS_KEY_JUST_PRESSED VK_KEY_L
                    OR IS_MOUSE_WHEEL_UP
                        SWITCH selected
                            CASE 1
                                //LeftLanes
                                CLEO_CALL getValueFromFlag 0 (flags, 8, 3) (value)
                                IF value < 15
                                    value += 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 8, 3, value) (flags)
                                ENDIF
                            BREAK
                            CASE 2
                                //RightLanes
                                CLEO_CALL getValueFromFlag 0 (flags, 11, 3) (value)
                                IF value < 15
                                    value += 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 11, 3, value) (flags)
                                ENDIF
                            BREAK
                            CASE 3
                                CLEO_CALL getValueFromFlag 0 (flags, 16, 2) value
                                IF value < 2
                                    value += 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 16, 2, value) flags
                                ENDIF
                            BREAK
                            CASE 4
                                math = 14  //Traffic Light Direction
                                SET_LOCAL_VAR_BIT_LVAR flags math
                            BREAK
                            CASE 5
                                math = 18  //Train
                                SET_LOCAL_VAR_BIT_LVAR flags math
                            BREAK
                            CASE 6
                                GET_CHAR_COORDINATES scplayer x y z

                                GET_GROUND_Z_FOR_3D_COORD x y z z
                                GET_WATER_HEIGHT_AT_COORDS x y TRUE fvalue

                                IF fvalue > z
                                    z = fvalue
                                ENDIF

                                z += 1.0
                                SET_OBJECT_COORDINATES object x y z
                                SET_OBJECT_COORDINATES curobject x y z

                                GET_CHAR_HEADING scplayer z
                                SET_OBJECT_ROTATION object 90.0 0.0 z
                            BREAK
                        ENDSWITCH
                    ENDIF

                    IF IS_KEY_JUST_PRESSED VK_KEY_J
                    OR IS_MOUSE_WHEEL_DOWN
                        SWITCH selected
                            CASE 1
                                //LeftLanes
                                CLEO_CALL getValueFromFlag 0 (flags, 8, 3) (value)
                                IF value > 0
                                    value -= 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 8, 3, value) (flags)
                                ENDIF
                            BREAK
                            CASE 2
                                //RightLanes
                                CLEO_CALL getValueFromFlag 0 (flags, 11, 3) (value)
                                IF value > 0
                                    value -= 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 11, 3, value) (flags)
                                ENDIF
                            BREAK
                            CASE 3
                                CLEO_CALL getValueFromFlag 0 (flags, 16, 2) value
                                IF value > 0
                                    value -= 1
                                    CLEO_CALL setFlagFromValue 0 (flags, 16, 2, value) flags
                                ENDIF
                            BREAK
                            CASE 4
                                math = 14  //Traffic Light Direction
                                CLEAR_LOCAL_VAR_BIT_LVAR flags math
                            BREAK
                            CASE 5
                                math = 18  //Train
                                CLEAR_LOCAL_VAR_BIT_LVAR flags math
                            BREAK
                        ENDSWITCH
                    ENDIF
                ENDIF

                SET_EXTENDED_OBJECT_VAR object VPEV 5 (flags)
            ENDIF
        ELSE //Nodes
            DRAW_STRING "LinkID:" DRAW_EVENT_AFTER_DRAWING 11.0 34.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "AreaID:" DRAW_EVENT_AFTER_DRAWING 9.0 47.0 0.3 0.65 TRUE FONT_MENU
            DRAW_STRING "NodeID:" DRAW_EVENT_AFTER_DRAWING 9.0 60.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Path Width:" DRAW_EVENT_AFTER_DRAWING 9.0 78.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "NodeType:" DRAW_EVENT_AFTER_DRAWING 9.0 102.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Traffic Level:" DRAW_EVENT_AFTER_DRAWING 7.0 124.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Highway:" DRAW_EVENT_AFTER_DRAWING 7.0 138.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Parking:" DRAW_EVENT_AFTER_DRAWING 7.0 152.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Spawn Chance:" DRAW_EVENT_AFTER_DRAWING 8.0 172.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Emergency Only:" DRAW_EVENT_AFTER_DRAWING 8.0 185.0 0.3 0.65 TRUE FONT_MENU

            DRAW_STRING "Floodfill:" DRAW_EVENT_AFTER_DRAWING 8.0 198.0 0.3 0.65 TRUE FONT_MENU

            IF NOT object = 0
                //Desenha os valores
                GET_EXTENDED_OBJECT_VAR object VPEV 1 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 34.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 2 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 47.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 3 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 59.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 4 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 77.0 0.3 0.65 TRUE FONT_MENU

                GET_EXTENDED_OBJECT_VAR object VPEV 5 value
                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 118.0 198.0 0.3 0.65 TRUE FONT_MENU

                //Desenha traffic level e spawn chance  
                IF DOES_OBJECT_EXIST object
                    math = 4
                    IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                        math = 5
                        IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                            DRAW_STRING "LOW" DRAW_EVENT_AFTER_DRAWING 115.0 124.0 0.3 0.65 TRUE FONT_MENU //1 1
                        ELSE
                            DRAW_STRING "HIGH" DRAW_EVENT_AFTER_DRAWING 115.0 124.0 0.3 0.65 TRUE FONT_MENU //1 0
                        ENDIF
                    ELSE
                        math = 5
                        IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                            DRAW_STRING "MEDIUM" DRAW_EVENT_AFTER_DRAWING 110.0 124.0 0.3 0.65 TRUE FONT_MENU //0 1
                        ELSE
                            DRAW_STRING "FULL" DRAW_EVENT_AFTER_DRAWING 115.0 124.0 0.3 0.65 TRUE FONT_MENU //0 0
                        ENDIF
                    ENDIF
                ENDIF

                CLEO_CALL getValueFromFlag 0 (flags, 16, 4) (value)
                STRING_FORMAT txt "%i/15" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 115.0 172.0 0.3 0.65 TRUE FONT_MENU

                //Desenha o sprite do nodetype
                IF isvehicle = 1
                    math = 7 //porque a porcaria ali não aceita valores absolutos
                    IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //boat
                        GET_TEXTURE_FROM_SPRITE 5 texture
                    ELSE
                        GET_TEXTURE_FROM_SPRITE 4 texture
                    ENDIF
                ELSE
                    GET_TEXTURE_FROM_SPRITE 3 texture
                ENDIF

                DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD 119.0 108.0 -52.0 12.5 0.0 1.0 TRUE 0 0 255 255 255 255

                //Desenha as booleanas
                math = 13
                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //highway
                    GET_TEXTURE_FROM_SPRITE 2 texture
                ELSE
                    GET_TEXTURE_FROM_SPRITE 1 texture
                ENDIF

                DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD 128.0 144.0 10.0 10.0 0.0 1.0 TRUE 0 0 255 255 255 255

                math = 21
                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //parking
                    GET_TEXTURE_FROM_SPRITE 2 texture
                ELSE
                    GET_TEXTURE_FROM_SPRITE 1 texture
                ENDIF

                DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD 128.0 158.0 10.0 10.0 0.0 1.0 TRUE 0 0 255 255 255 255

                math = 8
                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math //emergency
                    GET_TEXTURE_FROM_SPRITE 2 texture
                ELSE
                    GET_TEXTURE_FROM_SPRITE 1 texture
                ENDIF

                DRAW_TEXTURE_PLUS texture DRAW_EVENT_AFTER_HUD 119.0 192.0 10.0 10.0 0.0 1.0 TRUE 0 0 255 255 255 255

                //Desenha a caixa de seleção
                IF selected < 9
                    SWITCH selected
                        CASE 1
                            y = 84.0
                        BREAK
                        CASE 2
                            y = 108.0
                        BREAK
                        CASE 3
                            y = 130.0
                        BREAK
                        CASE 4
                            y = 143.0
                        BREAK
                        CASE 5
                            y = 158.0
                        BREAK
                        CASE 6
                            y = 178.0
                        BREAK
                        CASE 7
                            y = 191.0
                        BREAK
                        CASE 8
                            y = 204.0
                        BREAK
                    ENDSWITCH
                    DRAW_RECT 75.0 y 143.9 14.65 128 24 24 128
                ELSE
                    DRAW_RECT 75.0 242.0 145.9 50.35 128 24 24 128
                ENDIF
            ENDIF

            GET_SCRIPT_VAR manipulationScript 28 value
            IF value = 0
                IF IS_KEY_JUST_PRESSED VK_KEY_I
                    selected -= 1
                    IF selected = -1
                        selected = 9
                    ENDIF
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_KEY_K
                    selected += 1
                    IF selected = 10
                        selected = 0
                    ENDIF
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_INSERT
                    GET_LABEL_POINTER functionz count
                    WRITE_MEMORY count 4 (5) 0
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_DELETE
                    GET_LABEL_POINTER functionz count
                    WRITE_MEMORY count 4 (6) 0
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_KEY_L
                OR IS_MOUSE_WHEEL_UP
                    IF DOES_OBJECT_EXIST object
                        IF selected = 4
                            math = 13 //Highway
                            SET_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF
                        IF selected = 5
                            math = 21 //Parking
                            SET_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF
                        IF selected = 7
                            math = 8  //Emergency
                            SET_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF

                        //Floodfill
                        IF selected = 8
                            GET_EXTENDED_OBJECT_VAR object VPEV 5 value
                            value += 1
                            IF IS_CHAR_DUCKING scplayer
                                value += 9
                            ENDIF
                            IF value <= 255
                                SET_EXTENDED_OBJECT_VAR object VPEV 5 value
                            ENDIF
                        ENDIF

                        //Path Width
                        IF selected = 1
                            GET_EXTENDED_OBJECT_VAR object VPEV 4 value
                            value += 1
                            SET_EXTENDED_OBJECT_VAR object VPEV 4 value
                        ENDIF

                        //Traffic Level
                        IF selected = 3
                            math = 4
                            IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                math = 5
                                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                    math = 4
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math

                                    math = 5
                                    SET_LOCAL_VAR_BIT_LVAR flags math
                                ELSE
                                    math = 4
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math

                                    math = 5
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math
                                ENDIF
                            ELSE
                                math = 5
                                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                    math = 4
                                    SET_LOCAL_VAR_BIT_LVAR flags math

                                    math = 5
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math
                                ENDIF
                            ENDIF
                        ENDIF

                        //Spawn Chance
                        IF selected = 6
                            CLEO_CALL getValueFromFlag 0 (flags, 16, 4) (value)
                            IF value < 15
                                value += 1
                                CLEO_CALL setFlagFromValue 0 (flags, 16, 4, value) (flags)
                            ENDIF
                        ENDIF

                        //Boat
                        IF selected = 2
                        AND isvehicle = 1
                            math = 7
                            SET_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF

                        //Change Coordinates
                        IF selected = 9
                            GET_CHAR_COORDINATES scplayer x y z

                            GET_GROUND_Z_FOR_3D_COORD x y z z
                            GET_WATER_HEIGHT_AT_COORDS x y TRUE fvalue

                            IF fvalue > z
                                z = fvalue
                            ENDIF

                            SET_OBJECT_COORDINATES object x y z
                            SET_OBJECT_COORDINATES curobject x y z
                        ENDIF

                        SET_EXTENDED_OBJECT_VAR object VPEV 6 (flags)
                    ENDIF
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_KEY_J
                OR IS_MOUSE_WHEEL_DOWN
                    IF DOES_OBJECT_EXIST object
                        IF selected = 4
                            math = 13 //Highway
                            CLEAR_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF
                        IF selected = 5
                            math = 21 //Parking
                            CLEAR_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF
                        IF selected = 7
                            math = 8  //Emergency
                            CLEAR_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF

                        //Floodfill
                        GET_EXTENDED_OBJECT_VAR object VPEV 5 value
                        IF selected = 8
                        AND value > 0
                            value -= 1
                            IF IS_CHAR_DUCKING scplayer
                            AND value > 10
                            value -= 9
                            ENDIF
                            SET_EXTENDED_OBJECT_VAR object VPEV 5 value
                        ENDIF

                        //Path Width
                        GET_EXTENDED_OBJECT_VAR object VPEV 4 value
                        IF selected = 1
                        AND value > 0
                            value -= 1
                            SET_EXTENDED_OBJECT_VAR object VPEV 4 value
                        ENDIF

                        //Traffic Level
                        IF selected = 3
                            math = 4
                            IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                math = 5
                                IF NOT IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                    math = 4
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math
    
                                    math = 5
                                    SET_LOCAL_VAR_BIT_LVAR flags math
                                ENDIF
                            ELSE
                                math = 5
                                IF IS_LOCAL_VAR_BIT_SET_LVAR flags math
                                    math = 4
                                    SET_LOCAL_VAR_BIT_LVAR flags math

                                    math = 5
                                    SET_LOCAL_VAR_BIT_LVAR flags math
                                ELSE
                                    math = 4
                                    SET_LOCAL_VAR_BIT_LVAR flags math

                                    math = 5
                                    CLEAR_LOCAL_VAR_BIT_LVAR flags math
                                ENDIF
                            ENDIF
                        ENDIF

                        //Spawn Chance
                        IF selected = 6
                            CLEO_CALL getValueFromFlag 0 (flags, 16, 4) (value)
                            IF value > 0
                                value -= 1
                                CLEO_CALL setFlagFromValue 0 (flags, 16, 4, value) (flags)
                            ENDIF
                        ENDIF

                        //Boat
                        IF selected = 2
                        AND isvehicle = 1
                            math = 7
                            CLEAR_LOCAL_VAR_BIT_LVAR flags math
                        ENDIF

                     SET_EXTENDED_OBJECT_VAR object VPEV 6 (flags)
                     ENDIF
                ENDIF

                IF DOES_OBJECT_EXIST object
                    //Salva as definições da node atual na memória pra próxima a ser criada
                    GET_LABEL_POINTER savedVariables math

                    GET_EXTENDED_OBJECT_VAR object VPEV 4 value
                    WRITE_STRUCT_OFFSET math 32 4 (value) //PathWidth

                    GET_EXTENDED_OBJECT_VAR object VPEV 6 value
                    WRITE_STRUCT_OFFSET math 36 4 (value) //Flags
                ENDIF
            ENDIF
        ENDIF

        //Desenha zona atual
        DRAW_STRING "Current Zone:" DRAW_EVENT_AFTER_DRAWING 8.0 272.0 0.3 0.65 TRUE FONT_MENU
        STRING_FORMAT txt "%i" region
        DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 100.0 272.0 0.3 0.65 TRUE FONT_MENU

        //Desenha as coordenadas do Node
        DRAW_STRING "X:" DRAW_EVENT_AFTER_DRAWING 19.0 220.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Y:" DRAW_EVENT_AFTER_DRAWING 19.0 235.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Z:" DRAW_EVENT_AFTER_DRAWING 19.0 250.0 0.3 0.65 TRUE FONT_MENU
        
        IF DOES_OBJECT_EXIST object
            GET_OBJECT_COORDINATES object x y z

            STRING_FORMAT txt "%.3f" x
            DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 75.0 220.0 0.3 0.65 TRUE FONT_MENU

            STRING_FORMAT txt "%.3f" y
            DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 75.0 235.0 0.3 0.65 TRUE FONT_MENU

            STRING_FORMAT txt "%.3f" z
            DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 75.0 250.0 0.3 0.65 TRUE FONT_MENU
        ENDIF
    RETURN

    drawTab2:
        //Desenha a tabela inteira
        DRAW_TEXTURE_PLUS  0 DRAW_EVENT_AFTER_DRAWING 75.0 173.0 192.0 360.0 0.0 1.0 TRUE 0 0 20 20 20 200
        DRAW_STRING "Visual Path Editor by LightVelox" DRAW_EVENT_AFTER_DRAWING 56.0 333.0 0.15 0.325 TRUE FONT_MENU

        DRAW_STRING "Link Count:" DRAW_EVENT_AFTER_DRAWING 10.0 29.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Node:" DRAW_EVENT_AFTER_DRAWING 10.0 42.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Area" DRAW_EVENT_AFTER_DRAWING 7.0 62.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Node" DRAW_EVENT_AFTER_DRAWING 40.0 62.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Dist" DRAW_EVENT_AFTER_DRAWING 75.0 62.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "Navi" DRAW_EVENT_AFTER_DRAWING 105.0 62.0 0.3 0.65 TRUE FONT_MENU

        IF NOT object = 0
        AND NOT selecttype = 2
            //Desenha a caixa de seleção
            IF selected = 13
                DRAW_RECT 75.0 35.0 143.9 14.65 128 24 24 128
            ELSE
                IF selected > links
                    selected = links
                ENDIF

                IF selected > 0
                    selected -= 1
                    y =# selected
                    y *= 16.0
                    y += 84.0
                    DRAW_RECT 75.0 y 143.9 14.65 128 24 24 128
                    selected += 1
                ENDIF
            ENDIF

            //Desenha as infos sobre o node
            GET_EXTENDED_OBJECT_VAR object VPEV 2 value
            GET_EXTENDED_OBJECT_VAR object VPEV 3 value2

            STRING_FORMAT txt "%i-%i" value value2
            DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 85.0 42.0 0.3 0.65 TRUE FONT_MENU

            GET_SCRIPT_VAR manipulationScript 28 value
            IF value = 0
                IF IS_KEY_JUST_PRESSED VK_KEY_I
                    selected -= 1
                    IF selected < 0
                        selected = 0
                    ENDIF
                ENDIF

                IF IS_KEY_JUST_PRESSED VK_KEY_K
                    selected += 1
                ENDIF

                links += 1
                IF selected >= links
                    selected = 0
                ENDIF
                links -= 1
            ENDIF

            //Desenha os links
            count = 0
            WHILE count < links
                GET_LIST_VALUE_BY_INDEX linkarealist count value //AreaID
                GET_LIST_VALUE_BY_INDEX linknodelist count value2 //NodeId

                //Manda a info da node selecionado pro Manipulador
                selected -= 1
                IF count = selected
                    SET_SCRIPT_VAR manipulationScript 19 value
                    SET_SCRIPT_VAR manipulationScript 20 value2
                ENDIF
                selected += 1

                //Pega a posição do link atual na tela
                y =# count
                y *= 16.0
                y += 78.0

                STRING_FORMAT txt "%i" value
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 10.0 y 0.3 0.65 TRUE FONT_MENU //Area

                STRING_FORMAT txt "%i" value2
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 43.0 y 0.3 0.65 TRUE FONT_MENU //Node

                GET_LIST_VALUE_BY_INDEX linklenghtlist count value //Length
                GET_LIST_VALUE_BY_INDEX linknavilist count value2 //Navi

                //Distance and Navi Nodes
                fvalue =# value
                /*fvalue *= 6.25
                fvalue /= 100.0 Isso aqui converte de unidades de precisão para metros*/

                STRING_FORMAT txt "%.2f" fvalue
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 70.0 y 0.3 0.65 TRUE FONT_MENU //Dist

                value = 0 //AreaID
                math = 0 //NodeID

                //Navi do node selecionado
                CLEO_CALL getValueFromFlag 0 (value2, 10, 6) (value)
                CLEO_CALL getValueFromFlag 0 (value2, 0, 10) (math)

                //Manda a info da navi do node selecionado pro Manipulador
                selected -= 1
                IF count = selected
                    SET_SCRIPT_VAR manipulationScript 22 value
                    SET_SCRIPT_VAR manipulationScript 23 math
                ENDIF
                selected += 1

                //Desenha a info do NaviNode na UI
                STRING_FORMAT txt "%i-%i" value math //AreaID - NodeID
                DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 103.0 y 0.3 0.65 TRUE FONT_MENU //Navi

                count += 1
            ENDWHILE

            STRING_FORMAT txt "%i" links
            DRAW_STRING $txt DRAW_EVENT_AFTER_DRAWING 100.0 29.0 0.3 0.65 TRUE FONT_MENU
        ENDIF
    RETURN

    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////MANIPULATION////////////////////////////////////////////////////////
    {
    manipulationEditor:
    LVAR_FLOAT x y z
    LVAR_INT as ns mainscript createdFlag loading editingregion links value scplayer linklenghtlist linknavilist object loadedregions tab
    LVAR_INT count node areaid nodeid active naviarea navinode curobject navi selecttype visualScript submenu selected
    LVAR_TEXT_LABEL txt

    GET_PLAYER_CHAR 0 scplayer

    maniloop:
        WAIT 0

        IF IS_KEY_JUST_PRESSED VK_KEY_U
            submenu = 1
        ENDIF

        IF IS_KEY_JUST_PRESSED VK_KEY_I
            selected -= 1
        ENDIF
        IF IS_KEY_JUST_PRESSED VK_KEY_K
            selected += 1
        ENDIF

        SWITCH submenu
            CASE 1
                GOSUB drawSubmenu1
            BREAK
            CASE 2
                GOSUB drawSubmenu2
            BREAK
        ENDSWITCH

        IF active = FALSE
            GOTO end
        ENDIF

        IF IS_KEY_JUST_PRESSED VK_INSERT
        AND tab = 1
            GET_LABEL_POINTER functionz count
            WRITE_MEMORY count 4 (10) 0 //Desseleciona a node
            submenu = 2
        ENDIF

        USE_TEXT_COMMANDS 1

        IF loading = TRUE
            DRAW_STRING "LOADING" DRAW_EVENT_AFTER_DRAWING 240.0 400.0 1.0 1.8 TRUE FONT_MENU
        ENDIF

        IF NOT object = 0
        AND DOES_OBJECT_EXIST object
            IF selecttype = 2
                IF tab = 1
                    GET_EXTENDED_OBJECT_VAR object VPEV 1 (areaid)
                    GET_EXTENDED_OBJECT_VAR object VPEV 2 (nodeid)

                    CLEO_CALL getNodebyID 0 (loadedregions, areaid, nodeid) (node)
                    IF DOES_OBJECT_EXIST node
                        GET_OBJECT_COORDINATES node x y z
                        CONVERT_3D_TO_SCREEN_2D x y z TRUE TRUE x y z z

                        x -= 10.0
                        DRAW_STRING "N" DRAW_EVENT_AFTER_HUD x y 1.0 1.0 TRUE FONT_MENU
                    ENDIF

                    IF IS_KEY_JUST_PRESSED VK_KEY_P
                    OR IS_AIM_BUTTON_PRESSED PAD1
                        GET_LABEL_POINTER functionz count
                        WRITE_MEMORY count 4 (7) 0
                    ENDIF
                ENDIF
            ELSE
                IF tab = 1
                    //Tab 1
                    count = 0
                    WHILE count < links
                        GET_EXTENDED_OBJECT_VAR object VPEV 9 (areaid)
                        GET_EXTENDED_OBJECT_VAR object VPEV 10 (nodeid)
                        GET_LIST_VALUE_BY_INDEX areaid count areaid
                        GET_LIST_VALUE_BY_INDEX nodeid count nodeid

                        CLEO_CALL getNodebyID 0 (loadedregions, areaid, nodeid) (node)
                        IF DOES_OBJECT_EXIST node
                            GET_OBJECT_COORDINATES node x y z
                            CONVERT_3D_TO_SCREEN_2D x y z TRUE TRUE x y z z

                            STRING_FORMAT txt "%i" count
                            x -= 10.0
                            DRAW_STRING $txt DRAW_EVENT_AFTER_HUD x y 1.0 1.0 TRUE FONT_MENU
                        ENDIF

                        count += 1
                    ENDWHILE

                    IF NOT navi = 0
                        GET_OBJECT_COORDINATES navi x y z
                        CONVERT_3D_TO_SCREEN_2D x y z TRUE TRUE x y z z

                        x -= 10.0
                        DRAW_STRING "-N-" DRAW_EVENT_AFTER_HUD x y 0.75 0.75 TRUE FONT_MENU
                    ENDIF
                ELSE
                    //Tab 2
                    GET_SCRIPT_VAR visualScript 20 count
                    IF NOT count = 0
                        //Node do Link
                        CLEO_CALL getNodebyID 0 (loadedregions, areaid, nodeid) (node)
                        IF DOES_OBJECT_EXIST node
                            GET_OBJECT_COORDINATES node x y z
                            CONVERT_3D_TO_SCREEN_2D x y z TRUE TRUE x y z z

                            x -= 10.0
                            DRAW_STRING "N" DRAW_EVENT_AFTER_HUD x y 1.0 1.0 TRUE FONT_MENU
                        ENDIF

                        //Navi do Link
                        CLEO_CALL getNavibyID 0 (loadedregions, naviarea, navinode) (node)
                        IF DOES_OBJECT_EXIST node
                            GET_OBJECT_COORDINATES node x y z
                            CONVERT_3D_TO_SCREEN_2D x y z TRUE TRUE x y z z

                            x -= 10.0
                            DRAW_STRING "-N-" DRAW_EVENT_AFTER_HUD x y 1.0 1.0 TRUE FONT_MENU
                        ENDIF

                        //Deletar Links
                        IF IS_KEY_JUST_PRESSED VK_DELETE
                            count -= 1

                            GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                            LIST_REMOVE_INDEX as count

                            GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                            LIST_REMOVE_INDEX as count

                            GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                            LIST_REMOVE_INDEX as count

                            GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                            LIST_REMOVE_INDEX as count

                            links -= 1
                            SET_EXTENDED_OBJECT_VAR object VPEV 8 links
                            SET_SCRIPT_VAR visualScript 4 links

                            GET_SCRIPT_VAR mainscript 4 (as)
                            CLEO_CALL getListPointer 0 (3 as) (as)
                            READ_STRUCT_OFFSET as 16 4 (ns)
                            ns -= 1
                            WRITE_STRUCT_OFFSET as 16 4 (ns)
                            
                            count += 1
                        ENDIF

                        //Mover Link
                        IF IS_KEY_JUST_PRESSED VK_PRIOR //PageUp
                        OR IS_KEY_JUST_PRESSED VK_NEXT //PageDown
                            count -= 1

                            PRINT_FORMATTED_NOW "count: %i, links: %i" 1000 count links
                            IF links > 1
                                GET_LABEL_POINTER savedVariables ns

                                GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                                GET_LIST_VALUE_BY_INDEX as count (value)
                                WRITE_STRUCT_OFFSET ns 0 4 (value)

                                GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                                GET_LIST_VALUE_BY_INDEX as count (value)
                                WRITE_STRUCT_OFFSET ns 4 4 (value)

                                GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                                GET_LIST_VALUE_BY_INDEX as count (value)
                                WRITE_STRUCT_OFFSET ns 8 4 (value)

                                GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                                GET_LIST_VALUE_BY_INDEX as count (value)
                                WRITE_STRUCT_OFFSET ns 12 4 (value)

                                links -= 1
                                //Desce o link
                                IF IS_KEY_PRESSED VK_NEXT
                                AND count < links //Não é a última acima
                                    count += 1
                                    GOSUB updateLinkInfo
                                    count -= 1

                                    GOSUB updateLinkInfo2

                                    //Muda o link acima
                                    count += 1
                                    GOSUB updateLinkInfo3
                                    count -= 1

                                    count += 2
                                    SET_SCRIPT_VAR visualScript 20 count

                                    links += 1
                                    GOTO fimfds
                                ENDIF
                                links += 1

                                //Sobe o link
                                IF IS_KEY_PRESSED VK_PRIOR
                                AND count > 0 //Não é a primeira opção
                                    count -= 1
                                    GOSUB updateLinkInfo
                                    count += 1

                                    GOSUB updateLinkInfo2

                                    //Muda o link abaixo
                                    count -= 1
                                    GOSUB updateLinkInfo3
                                    count += 1

                                    count -= 0
                                    SET_SCRIPT_VAR visualScript 20 count
                                ENDIF
                            ENDIF
                            count += 1
                        ENDIF

                        //Atualizar Link
                        IF IS_KEY_JUST_PRESSED VK_KEY_P
                        OR IS_AIM_BUTTON_PRESSED PAD1
                            GET_LABEL_POINTER functionz count
                            WRITE_MEMORY count 4 (4) 0
                        ENDIF

                        GOTO fimfds

                        updateLinkInfo:
                            GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                            GET_LIST_VALUE_BY_INDEX as count (value)
                            WRITE_STRUCT_OFFSET ns 16 4 (value)

                            GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                            GET_LIST_VALUE_BY_INDEX as count (value)
                            WRITE_STRUCT_OFFSET ns 20 4 (value)

                            GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                            GET_LIST_VALUE_BY_INDEX as count (value)
                            WRITE_STRUCT_OFFSET ns 24 4 (value)

                            GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                            GET_LIST_VALUE_BY_INDEX as count (value)
                            WRITE_STRUCT_OFFSET ns 28 4 (value)
                        RETURN

                        updateLinkInfo2:
                            GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                            READ_STRUCT_OFFSET ns 16 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                            READ_STRUCT_OFFSET ns 20 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                            READ_STRUCT_OFFSET ns 24 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                            READ_STRUCT_OFFSET ns 28 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)
                        RETURN

                        updateLinkInfo3:
                            GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                            READ_STRUCT_OFFSET ns 0 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                            READ_STRUCT_OFFSET ns 4 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                            READ_STRUCT_OFFSET ns 8 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)

                            GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                            READ_STRUCT_OFFSET ns 12 4 (value)
                            CLEO_CALL setListValue 0 (as, count, value) (as)
                        RETURN

                        fimfds:
                    ENDIF

                    //Criar Links
                    IF IS_KEY_JUST_PRESSED VK_INSERT
                        count -= 1

                        IF links < 16
                            links += 1
                            SET_SCRIPT_VAR visualScript 4 links
                            SET_EXTENDED_OBJECT_VAR object VPEV 8 links

                            GET_EXTENDED_OBJECT_VAR object VPEV 9 (as) //AreaID
                            LIST_ADD as 0

                            GET_EXTENDED_OBJECT_VAR object VPEV 10 (as) //NodeId
                            LIST_ADD as -1

                            GET_EXTENDED_OBJECT_VAR object VPEV 11 (as) //Navi
                            LIST_ADD as 0

                            GET_EXTENDED_OBJECT_VAR object VPEV 12 (as) //Length
                            LIST_ADD as 0

                            GET_SCRIPT_VAR mainscript 4 (as)
                            CLEO_CALL getListPointer 0 (3 as) (as)
                            READ_STRUCT_OFFSET as 16 4 (ns)
                            ns += 1
                            WRITE_STRUCT_OFFSET as 16 4 (ns)
                        ENDIF

                        count += 1
                    ENDIF
                ENDIF
            ENDIF
        ENDIF
        end:
        USE_TEXT_COMMANDS 0
    GOTO maniloop

    drawSubmenu1:
        DRAW_TEXTURE_PLUS 0 DRAW_EVENT_AFTER_DRAWING 557.0 145.0 57.0 63.2 0.0 1.0 TRUE 0 0 20 20 20 200
        DRAW_STRING "SAVE" DRAW_EVENT_AFTER_DRAWING 544.0 119.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "LOAD" DRAW_EVENT_AFTER_DRAWING 544.0 132.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "UNLOAD" DRAW_EVENT_AFTER_DRAWING 540.0 145.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "CLOSE" DRAW_EVENT_AFTER_DRAWING 542.0 158.0 0.3 0.65 TRUE FONT_MENU

        IF selected > 3
            selected = 3
        ELSE
            IF selected < 0
                selected = 0
            ENDIF
        ENDIF

        SWITCH selected
            CASE 0
                DRAW_RECT 557.0 125.0 42.9 13.4 128 24 24 128
            BREAK
            CASE 1
                DRAW_RECT 557.0 138.0 42.9 13.4 128 24 24 128
            BREAK
            CASE 2
                DRAW_RECT 557.0 151.0 42.9 13.4 128 24 24 128
            BREAK
            CASE 3
                DRAW_RECT 557.0 164.0 42.9 13.4 128 24 24 128
            BREAK
        ENDSWITCH

        IF IS_KEY_JUST_PRESSED VK_KEY_L
        OR IS_MOUSE_WHEEL_UP
            GET_LABEL_POINTER functionz count
            SWITCH selected
                CASE 0
                    WRITE_MEMORY count 4 (1) 0
                BREAK
                CASE 1
                    WRITE_MEMORY count 4 (2) 0
                BREAK
                CASE 2
                    WRITE_MEMORY count 4 (3) 0
                BREAK
            ENDSWITCH
            submenu = 0
        ENDIF
    RETURN

    drawSubmenu2:
        DRAW_TEXTURE_PLUS 0 DRAW_EVENT_AFTER_DRAWING 557.0 145.0 75.0 63.2 0.0 1.0 TRUE 0 0 20 20 20 200
        DRAW_STRING "PEDNODE" DRAW_EVENT_AFTER_DRAWING 536.0 119.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "VEHNODE" DRAW_EVENT_AFTER_DRAWING 536.0 133.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "NAVI" DRAW_EVENT_AFTER_DRAWING 546.0 145.0 0.3 0.65 TRUE FONT_MENU
        DRAW_STRING "CLOSE" DRAW_EVENT_AFTER_DRAWING 542.0 158.0 0.3 0.65 TRUE FONT_MENU

        IF selected > 3
            selected = 3
        ELSE
            IF selected < 0
                selected = 0
            ENDIF
        ENDIF

        SWITCH selected
            CASE 0
                DRAW_RECT 557.0 125.0 57.0 13.4 128 24 24 128
            BREAK
            CASE 1
                DRAW_RECT 557.0 138.0 57.0 13.4 128 24 24 128
            BREAK
            CASE 2
                DRAW_RECT 557.0 151.0 57.0 13.4 128 24 24 128
            BREAK
            CASE 3
                DRAW_RECT 557.0 164.0 57.0 13.4 128 24 24 128
            BREAK
        ENDSWITCH

        IF IS_KEY_JUST_PRESSED VK_KEY_L
        OR IS_MOUSE_WHEEL_UP
            GET_LABEL_POINTER functionz count
            SWITCH selected
                CASE 0
                    WRITE_MEMORY count 4 (8) 0
                BREAK
                CASE 1
                    WRITE_MEMORY count 4 (5) 0
                BREAK
                CASE 2
                    WRITE_MEMORY count 4 (9) 0
                BREAK
                CASE 3
                    submenu = 0
                BREAK
            ENDSWITCH
        ENDIF
    RETURN
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////ALGORITHMS/////////////////////////////////////////////////////////
    {
    getRegion:
        LVAR_FLOAT xx yy
        LVAR_INT region

        CALL_FUNCTION_RETURN 0x44D830 2 0 (yy, xx) (region)
    CLEO_RETURN 0 region
    }

    {
    WriteFileOffset:
        LVAR_INT file offset size var

        FILE_SEEK file offset 0
        WRITE_TO_FILE file size var
    CLEO_RETURN 0
    }

    {
    convertToCoord:
        LVAR_INT value
        LVAR_FLOAT var

        IF value > 32766
            value -= 65536
        ENDIF

        var =# value
        var /= 8.0
    CLEO_RETURN 0 var
    }

    {
    convertToSignedCoord:
        LVAR_FLOAT var
        LVAR_INT value

        var *= 8.0
        value =# var
        IF value < 0
            value += 65536
        ENDIF
    CLEO_RETURN 0 value
    }

    {
    getRadianAngle:
        LVAR_INT x y
        LVAR_FLOAT xx yy angle tx ty

        xx =# x

        yy =# y

        xx /= 100.0
        yy /= 100.0

        GET_HEADING_FROM_VECTOR_2D xx yy angle
    CLEO_RETURN 0 angle
    }

    {
    getSignedAngle:
        LVAR_FLOAT angle x y dist
        LVAR_INT tx ty

        SIN angle x
        COS angle y
        
        y *= 100.0
        x *= 100.0

        ty =# y
        ty *= -1

        IF ty < -100
            ty += 256
        ENDIF

        tx =# x

        IF tx < -100
            tx += 256
        ENDIF
    CLEO_RETURN 0 tx ty
    }

    {
    getNodeOffset:
        LVAR_INT node offset

        offset = 28 * node
        offset += 20
    CLEO_RETURN 0 offset
    }

    {
    getNaviOffset:
        LVAR_INT navi nodes math offset

        offset = 14 * navi
        offset += 20
        math = 28 * nodes
        offset += math
    CLEO_RETURN 0 offset
    }

    {
    getLinkOffset:
        LVAR_INT link nodes navis math offset
        math = 20

        offset += math
        math = 28 * nodes
        offset += math
        math = 14 * navis
        offset += math
        math = 4 * link
        offset += math
    CLEO_RETURN 0 offset
    }

    {
    getNaviLinkOffset:
        LVAR_INT navilink nodes navis links math offset

        offset = 2 * navilink
        math = 4 * links
        offset += math
        math = 28 * nodes
        offset += math
        math = 14 * navis
        offset += math
        offset += 768
        offset += 20
    CLEO_RETURN 0 offset
    }

    {
    getLinkLengthOffset:
        LVAR_INT linklength nodes navis links math offset

        offset = 2 * links
        math = 4 * links
        offset += math
        math = 28 * nodes
        offset += math
        math = 14 * navis
        offset += math
        offset += 768
        offset += 20
        offset += linklength
    CLEO_RETURN 0 offset
    }

    {
    getIntersectionOffset:
        LVAR_INT intersection nodes navis links math offset

        offset = links
        math = 2 * links
        offset += math
        math = 4 * links
        offset += math
        math = 28 * nodes
        offset += math
        math = 14 * navis
        offset += math
        offset += 768
        offset += 20
    CLEO_RETURN 0 offset
    }

    
    {
    getValueFromFlag:
        LVAR_INT flag begin number offset count math value

        math = 1
        WHILE count < number
            offset = begin + count
            IF IS_LOCAL_VAR_BIT_SET_LVAR flag offset
                value += math
            ENDIF
            math *= 2 
            count += 1
        ENDWHILE
    CLEO_RETURN 0 value
    }

    {
    setFlagFromValue:
        LVAR_INT flag begin number value offset count
        
        WHILE count < number
            offset = begin + count
            IF IS_LOCAL_VAR_BIT_SET_LVAR value count
                SET_LOCAL_VAR_BIT_LVAR flag offset
            ELSE
                CLEAR_LOCAL_VAR_BIT_LVAR flag offset
            ENDIF
            count += 1
        ENDWHILE
    CLEO_RETURN 0 flag
    }

    {
    setListValue:
        LVAR_INT list index value copy size count var

        CREATE_LIST DATATYPE_INT copy
        GET_LIST_SIZE list size
        count = 0
        WHILE count < size
            GET_LIST_VALUE_BY_INDEX list count var
            LIST_ADD copy var
            count += 1
        ENDWHILE

        RESET_LIST list
        count = 0
        WHILE count < size
            IF count = index
                LIST_ADD list value
            ELSE
                GET_LIST_VALUE_BY_INDEX copy count var
                LIST_ADD list var
            ENDIF
            count += 1
        ENDWHILE
        DELETE_LIST copy
    CLEO_RETURN 0 list
    }

    {
    addListValue:
        LVAR_INT list index value copy size count var

        CREATE_LIST DATATYPE_INT copy
        GET_LIST_SIZE list size
        count = 0
        WHILE count < size
            GET_LIST_VALUE_BY_INDEX list count var
            LIST_ADD copy var
            count += 1
        ENDWHILE

        RESET_LIST list
        count = 0
        WHILE count < size
            IF count = index
                LIST_ADD list value
                LIST_ADD list var
            ELSE
                GET_LIST_VALUE_BY_INDEX copy count var
                LIST_ADD list var
            ENDIF
            count += 1
        ENDWHILE
        DELETE_LIST copy
    CLEO_RETURN 0 list
    }

    {
    getListPointer:
        LVAR_INT type nb offset math memory
        GET_LABEL_POINTER lists memory

        math = 36
        math *= type

        offset = nb
        offset *= 4
        offset += math

        memory += offset
    CLEO_RETURN 0 memory
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////NODECHANGE/////////////////////////////////////////////////////////
    {
    addRegionNoDuplicates:
        LVAR_INT reg list size count var found

        count = 0
        found = 0
        GET_LIST_SIZE list size
        WHILE count < size
            GET_LIST_VALUE_BY_INDEX list count var
            IF var = reg
                found = 1
            ENDIF
            count += 1
        ENDWHILE

        IF found = 0
        AND reg >= 0
        AND reg <= 64
            LIST_ADD list reg
        ENDIF
    CLEO_RETURN 0
    }

    {
    getNodebyID:
        LVAR_INT loadedregions areaid nodeid node count region list count2 obj objid size

        IF NOT loadedregions = 0
        AND nodeid >= 0
            node = 0
            REPEAT 9 count
                GET_LIST_VALUE_BY_INDEX loadedregions count region
                IF region = areaid
                    CLEO_CALL getListPointer 0 (1 count) (list) //NodeList
                    READ_MEMORY list 4 0 list

                    GET_LIST_SIZE list size
                    IF NOT list = 0
                    AND nodeid < size
                        GET_LIST_VALUE_BY_INDEX list nodeid (node)
                    ENDIF
                ENDIF
            ENDREPEAT
        ENDIF
    CLEO_RETURN 0 node
    }

    {
    getNavibyID:
        LVAR_INT loadedregions areaid nodeid node count region list size

        IF NOT loadedregions = 0
        AND nodeid >= 0
            node = 0
            REPEAT 9 count
                GET_LIST_VALUE_BY_INDEX loadedregions count region
                IF region = areaid
                    CLEO_CALL getListPointer 0 (2 count) (list) //NaviList
                    READ_MEMORY list 4 0 list

                    GET_LIST_SIZE list size
                    IF NOT list = 0
                    AND nodeid < size
                        GET_LIST_VALUE_BY_INDEX list nodeid (node)
                    ENDIF
                ENDIF
            ENDREPEAT
        ENDIF
    CLEO_RETURN 0 node
    }

    {
    removeCurrentNode:
        LVAR_INT nodeobj curfile selecttype loadedregions list list2 list3 list4 list5 list6 areaid nodeid obj origid vehNodeCount naviCount linkCount
        LVAR_INT nodeCount filemem count count2 links value value2 size size2 region currentid

        //Pega a ref pras lists globais de contagem de nodes por arquivo
        CLEO_CALL getListPointer 0 (3 curfile) (list) //FileList
        READ_MEMORY list 4 0 filemem

        //Pega a curfile da regiao da node atual
        IF selecttype = 1
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 2 areaid
        ELSE
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 1 areaid
        ENDIF
        CLEO_CALL getRegionCurfile 0 (areaid, loadedregions) (curfile)

        //Caso não seja da regiao central não se pode destruir a node
        IF NOT curfile = 8
            PRINT_STRING_NOW "So se pode excluir ou adicionar na regiao inicial" 3000
            CLEO_RETURN 0
        ENDIF

        region = areaid

        READ_STRUCT_OFFSET filemem 0 4 (nodeCount)
        READ_STRUCT_OFFSET filemem 4 4 (vehNodeCount)
        READ_STRUCT_OFFSET filemem 12 4 (naviCount)
        READ_STRUCT_OFFSET filemem 16 4 (linkCount)

        IF selecttype = 1
            //Nodes
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 3 origid //NodeID
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 8 links
            linkCount -= links

            //Remove da Lista de Objetos e Nodes
            CLEO_CALL getListPointer 0 (0 curfile) (list) //ObjectList
            READ_MEMORY list 4 0 list
            LIST_REMOVE_INDEX list origid

            CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
            READ_MEMORY list 4 0 list
            LIST_REMOVE_INDEX list origid

            //Atualiza os IDs dos nodes
            GET_LIST_SIZE list size

            count = 0
            currentid = 0
            WHILE count < size
                GET_LIST_VALUE_BY_INDEX list count (obj)
                IF DOES_OBJECT_EXIST obj
                    GET_EXTENDED_OBJECT_VAR obj VPEV 2 areaid
                    GET_EXTENDED_OBJECT_VAR obj VPEV 3 nodeid

                    //Atualiza o NodeID
                    IF areaid = region
                    AND nodeid > origid
                        nodeid -= 1
                        SET_EXTENDED_OBJECT_VAR obj VPEV 3 nodeid
                    ENDIF

                    //Remove todos os links pra cá
                    GET_EXTENDED_OBJECT_VAR obj VPEV 8 links //Links
                    count2 = 0
                    IF NOT links = 0 //Crash?
                        WHILE count2 < links
                            GET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2) //AreaList
                            GET_LIST_VALUE_BY_INDEX list2 count2 areaid

                            GET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3) //NodeList
                            GET_LIST_VALUE_BY_INDEX list3 count2 nodeid

                            GET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4) //LinkNaviList
                            GET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5) //LinkNaviList
                            GET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6) //LinkObjectList

                            GET_LIST_VALUE_BY_INDEX list2 count2 areaid
                            GET_LIST_VALUE_BY_INDEX list3 count2 nodeid

                            IF nodeid = origid
                            AND areaid = region
                                LIST_REMOVE_INDEX list2 count2
                                LIST_REMOVE_INDEX list3 count2
                                LIST_REMOVE_INDEX list4 count2
                                LIST_REMOVE_INDEX list5 count2
                                LIST_REMOVE_INDEX list6 count2

                                //Atualiza todas as Extended Vars
                                links -= 1
                                linkCount -= 1
                                SET_EXTENDED_OBJECT_VAR obj VPEV 8 links
                                SET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2)
                                SET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                                SET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4)
                                SET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5)
                                SET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6)
                            ENDIF
                            count2 += 1
                        ENDWHILE
                    ENDIF

                    //Atualiza os LinkIDs das nodes
                    SET_EXTENDED_OBJECT_VAR obj VPEV 1 currentid
                    currentid += links

                    //Atualiza todos os links das nodes
                    count2 = 0
                    WHILE count2 < links
                        GET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2) //Arealist
                        GET_LIST_VALUE_BY_INDEX list2 count2 areaid

                        GET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3) //NodeList
                        GET_LIST_VALUE_BY_INDEX list3 count2 nodeid

                        IF nodeid >= origid
                        AND areaid = region
                            nodeid -= 1
                            CLEO_CALL setListValue 0 (list3, count2, nodeid) (list3)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                        ENDIF

                        count2 += 1
                    ENDWHILE
                ENDIF
                count += 1
            ENDWHILE

            //Atualiza os Links das nodes interregionais
            interregionupdate:
            GET_LABEL_POINTER interregion list
            READ_MEMORY list 4 0 list
            GET_LIST_SIZE list size

            count = 0
            WHILE count < size
                GET_LIST_VALUE_BY_INDEX list count (obj)
                IF DOES_OBJECT_EXIST obj
                    GET_EXTENDED_OBJECT_VAR obj VPEV 8 (links)
                    GET_EXTENDED_OBJECT_VAR obj VPEV 2 (value) //AreaID dessa node

                    count2 = 0
                    WHILE count2 < links
                        GET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2) //AreaList
                        GET_LIST_VALUE_BY_INDEX list2 count2 areaid

                        GET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3) //NodeList
                        GET_LIST_VALUE_BY_INDEX list3 count2 nodeid

                        IF areaid = region
                        AND nodeid = origid
                        AND NOT areaid = value
                            GET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4) //LinkNaviList
                            GET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5) //LinkNaviList
                            GET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6) //LinkObjectList

                            LIST_REMOVE_INDEX list2 count2
                            LIST_REMOVE_INDEX list3 count2
                            LIST_REMOVE_INDEX list4 count2
                            LIST_REMOVE_INDEX list5 count2
                            LIST_REMOVE_INDEX list6 count2
                            links -= 1

                            SET_EXTENDED_OBJECT_VAR obj VPEV 8 links
                            SET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6)
                            GOTO interregionupdate
                        ENDIF

                        IF areaid = region
                        AND nodeid >= origid
                        AND NOT areaid = value
                            nodeid -= 1
                            CLEO_CALL setListValue 0 list3 count2 nodeid (list3)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 10 list3
                        ENDIF
                        count2 += 1
                    ENDWHILE
                ENDIF
                count += 1
            ENDWHILE

            //Atualiza as referências dos NaviNodes
            IF origid < vehNodeCount
                CLEO_CALL getListPointer 0 (2 curfile) (list) //NaviList
                READ_MEMORY list 4 0 list
                GET_LIST_SIZE list size

                count = 0
                WHILE count < size
                    GET_LIST_VALUE_BY_INDEX list count (obj)
                    IF DOES_OBJECT_EXIST obj
                        GET_EXTENDED_OBJECT_VAR obj VPEV 1 (areaid) //AreaID
                        GET_EXTENDED_OBJECT_VAR obj VPEV 2 (nodeid) //NodeID

                        IF areaid = region
                            //Caso seja um ID superior a node excluída, corrija o id
                            IF nodeid > origid
                                nodeid -= 1
                            ELSE //Caso seja a node excluída, seta a nodeid pra -1, prevenindo o mapa de ser compilado
                                nodeid = -1
                            ENDIF
                            SET_EXTENDED_OBJECT_VAR obj VPEV 2 (nodeid)
                        ENDIF
                    ENDIF
                    count += 1
                ENDWHILE

                vehNodeCount -=1
            ENDIF
            nodeCount -= 1

            DELETE_OBJECT nodeobj
        ELSE
            //NaviNode
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 7 origid

            count2 = nodeCount
            count2 += origid
            CLEO_CALL getListPointer 0 (0 curfile) (list) //ObjectList
            READ_MEMORY list 4 0 list
            LIST_REMOVE_INDEX list count2

            CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
            READ_MEMORY list 4 0 list
            GET_LIST_SIZE list size

            //Removes todos os Links pra cá
            count = 0
            WHILE count < size
                GET_LIST_VALUE_BY_INDEX list count (obj)
                IF DOES_OBJECT_EXIST obj
                    GET_EXTENDED_OBJECT_VAR obj VPEV 8 links //Links

                    count2 = 0
                    WHILE count2 < links
                        GET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2) //AreaList
                        GET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3) //NodeList
                        GET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4) //LinkNaviList
                        GET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5) //LinkNaviList
                        GET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6) //LinkObjectList

                        GET_LIST_VALUE_BY_INDEX list4 count2 (value)
                        CLEO_CALL getValueFromFlag 0 (value, 0, 10) (nodeid)
                        CLEO_CALL getValueFromFlag 0 (value, 10, 6) (areaid)

                        IF areaid = region
                        AND nodeid = origid
                            LIST_REMOVE_INDEX list2 count2
                            LIST_REMOVE_INDEX list3 count2
                            LIST_REMOVE_INDEX list4 count2
                            LIST_REMOVE_INDEX list5 count2
                            LIST_REMOVE_INDEX list6 count2
                            links -= 1
                            SET_EXTENDED_OBJECT_VAR obj VPEV 8 links
                        ENDIF

                        SET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2)
                        SET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                        SET_EXTENDED_OBJECT_VAR obj VPEV 11 (list4)
                        SET_EXTENDED_OBJECT_VAR obj VPEV 12 (list5)
                        SET_EXTENDED_OBJECT_VAR obj VPEV 13 (list6)

                        count2 += 1
                    ENDWHILE
                ENDIF
                count += 1
            ENDWHILE

            //Atualiza os IDs dos links pros NaviNodes
            GET_EXTENDED_OBJECT_VAR nodeobj VPEV 1 (value2) //AreaID
            count = 0
            WHILE count < size
                GET_LIST_VALUE_BY_INDEX list count (obj)
                IF DOES_OBJECT_EXIST obj
                    GET_EXTENDED_OBJECT_VAR obj VPEV 11 list2 //LinkNaviList
                    GET_LIST_SIZE list2 size2

                    count2 = 0
                    WHILE count2 < size2
                        GET_LIST_VALUE_BY_INDEX list2 count2 (value)
                        CLEO_CALL getValueFromFlag 0 (value, 0, 10) (nodeid)
                        CLEO_CALL getValueFromFlag 0 (value, 10, 6) (areaid)

                        IF areaid = value2
                        AND nodeid > origid
                            nodeid -= 1
                            CLEO_CALL setFlagFromValue 0 (value, 0, 10, nodeid) (value)
                            CLEO_CALL setListValue 0 list2 count2 value (list2)
                            SET_EXTENDED_OBJECT_VAR obj VPEV 11 list2
                        ENDIF
                        count2 += 1
                    ENDWHILE
                ENDIF
                count += 1
            ENDWHILE

            CLEO_CALL getListPointer 0 (2 curfile) (list) //NaviList
            READ_MEMORY list 4 0 list
            LIST_REMOVE_INDEX list origid
            GET_LIST_SIZE list size

            //Atualiza os IDs dos Navi Nodes
            count = 0
            WHILE count < size
                GET_LIST_VALUE_BY_INDEX list count (obj)
                IF DOES_OBJECT_EXIST obj
                    GET_EXTENDED_OBJECT_VAR obj VPEV 7 nodeid
                    IF nodeid > origid
                        nodeid -= 1
                        SET_EXTENDED_OBJECT_VAR obj VPEV 7 nodeid
                    ENDIF
                ENDIF
                count += 1
            ENDWHILE

            DELETE_OBJECT nodeobj
            naviCount -= 1
        ENDIF

        WRITE_STRUCT_OFFSET filemem 0 4 (nodeCount)
        WRITE_STRUCT_OFFSET filemem 4 4 (vehNodeCount)
        WRITE_STRUCT_OFFSET filemem 12 4 (naviCount)
        WRITE_STRUCT_OFFSET filemem 16 4 (linkCount)
    CLEO_RETURN 0
    }

    {
    createNewNode:
        LVAR_INT type loadedregions //types = 0(car), 1(ped), 2(boat), 3(navi)
        LVAR_FLOAT x y z angle

        LVAR_INT count count2 links region obj size scplayer listptr areaid nodeid list objlist nodelist nodeCount vehNodeCount naviCount
        LVAR_INT linkCount object flags flagpos list2 list3 curfile

        GET_PLAYER_CHAR 0 scplayer

        //Pega a curfile da regiao da node atual
        CLEO_CALL getRegion 0 (x, y, region)
        CLEO_CALL getRegionCurfile 0 (region, loadedregions) (curfile)

        //Caso não seja da regiao central não se pode destruir a node
        IF NOT curfile = 8
            PRINT_STRING_NOW "So se pode excluir ou adicionar na regiao inicial" 3000
            CLEO_RETURN 0
        ENDIF

        CLEO_CALL getListPointer 0 (0 curfile) (listptr) //ObjectList
        READ_MEMORY listptr 4 0 (objlist)

        CLEO_CALL getListPointer 0 (1 curfile) (listptr) //NodeList
        READ_MEMORY listptr 4 0 (nodelist)

        //Carrega os modelos de visualização
        GET_LABEL_POINTER modelIds count
        READ_STRUCT_OFFSET count 0 4 (count2)
        REQUEST_MODEL count2

        READ_STRUCT_OFFSET count 4 4 (count2)
        REQUEST_MODEL count2

        READ_STRUCT_OFFSET count 12 4 (count2)
        REQUEST_MODEL count2
        
        LOAD_ALL_MODELS_NOW

        CLEO_CALL getListPointer 0 (3 curfile) (list) //FileList
        READ_MEMORY list 4 0 listptr

        //Pega as variáveis do cabeçalho
        READ_STRUCT_OFFSET listptr 0 4 (nodeCount)
        READ_STRUCT_OFFSET listptr 4 4 (vehNodeCount)
        READ_STRUCT_OFFSET listptr 12 4 (naviCount)
        READ_STRUCT_OFFSET listptr 16 4 (linkCount)

        z -= 0.5
        SWITCH type
            CASE 0
            CASE 2
                READ_STRUCT_OFFSET count 4 4 (count2)
                CREATE_OBJECT_NO_SAVE count2 x y z TRUE FALSE object //Car-boat
            BREAK
            CASE 1
                READ_STRUCT_OFFSET count 0 4 (count2)
                CREATE_OBJECT_NO_SAVE count2 x y z TRUE FALSE object //Ped
            BREAK
            CASE 3
                READ_STRUCT_OFFSET count 12 4 (count2)
                CREATE_OBJECT_NO_SAVE count2 x y z TRUE FALSE object //Navi
            BREAK
        ENDSWITCH

        SET_OBJECT_COLLISION object 0

        CLEO_CALL getListPointer 0 (0 curfile) (listptr) //ObjectList
        READ_MEMORY listptr 4 0 list
        LIST_ADD list object

        CLEO_CALL getRegion 0 (x, y) region

        IF type < 3
            //Nodes
            INIT_EXTENDED_OBJECT_VARS object VPEV 13

            IF type = 1 //Ped
                SET_EXTENDED_OBJECT_VAR object VPEV 3 nodeCount //NodeID
                LIST_ADD nodelist object
            ELSE
                count2 = nodeCount - vehNodeCount
                
                //Checa se existem nodes de pedestres primeiro
                IF count2 > 0
                    CLEO_CALL addListValue 0 (nodelist, vehNodeCount, object) (nodelist)

                    //Atualiza todos os NodeIDs dos pednodes
                    GET_LIST_SIZE nodelist size

                    count = 0
                    WHILE count < size
                        GET_LIST_VALUE_BY_INDEX nodelist count (obj)
                        IF DOES_OBJECT_EXIST obj
                            GET_EXTENDED_OBJECT_VAR obj VPEV 3 nodeid

                            IF nodeid >= vehNodeCount
                                nodeid += 1
                                SET_EXTENDED_OBJECT_VAR obj VPEV 3 nodeid

                                GET_EXTENDED_OBJECT_VAR obj VPEV 8 links

                                count2 = 0
                                WHILE count2 < links
                                    GET_EXTENDED_OBJECT_VAR obj VPEV 9 (list2)
                                    GET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                                    GET_LIST_VALUE_BY_INDEX list2 count2 areaid
                                    GET_LIST_VALUE_BY_INDEX list3 count2 nodeid

                                    IF areaid = region
                                        nodeid += 1
                                        CLEO_CALL setListValue 0 (list3, count2, nodeid) list3
                                        SET_EXTENDED_OBJECT_VAR obj VPEV 10 (list3)
                                    ENDIF
                                    count2 += 1
                                ENDWHILE
                            ENDIF
                        ENDIF
                        count += 1
                    ENDWHILE
                ELSE
                    LIST_ADD list object
                ENDIF

                SET_EXTENDED_OBJECT_VAR object VPEV 3 vehNodeCount
                vehNodeCount += 1
            ENDIF

            SET_EXTENDED_OBJECT_VAR object VPEV 1 linkCount //LinkID

            SET_EXTENDED_OBJECT_VAR object VPEV 2 region //AreaID

            IF type = 1
                SET_EXTENDED_OBJECT_VAR object VPEV 4 16 //Path Width Peds
            ELSE
                SET_EXTENDED_OBJECT_VAR object VPEV 4 0 //Path Width Vehicles
            ENDIF

            SWITCH type
                CASE 0
                    SET_EXTENDED_OBJECT_VAR object VPEV 5 1 //FloodFill Vehicles
                BREAK
                CASE 1
                    SET_EXTENDED_OBJECT_VAR object VPEV 5 5 //FloodFill Peds, o único que não é fixo
                BREAK
                CASE 2
                    SET_EXTENDED_OBJECT_VAR object VPEV 5 2 //FloodFill Boats
                BREAK
            ENDSWITCH

            //SetFlags
                flags = 0

                //Vehicles
                IF type = 0
                OR type = 2
                    flagpos = 12 //Is not Highway
                    SET_LOCAL_VAR_BIT_LVAR flags flagpos

                    IF type = 2 //Boat
                        flagpos = 7
                        SET_LOCAL_VAR_BIT_LVAR flags flagpos
                    ENDIF
                ENDIF

                //16-19 = Spawn probability
                flagpos = 16
                SET_LOCAL_VAR_BIT_LVAR flags flagpos
                
                flagpos = 17
                SET_LOCAL_VAR_BIT_LVAR flags flagpos

                flagpos = 18
                SET_LOCAL_VAR_BIT_LVAR flags flagpos

                flagpos = 19
                SET_LOCAL_VAR_BIT_LVAR flags flagpos
            //

            SET_EXTENDED_OBJECT_VAR object VPEV 6 flags //Flags

            //Number of Links
            SET_EXTENDED_OBJECT_VAR object VPEV 8 2 //Links, maioria das nodes crasha se não tiver pelo menos 2 links, vlw rockstar!

            CREATE_LIST DATATYPE_INT list
            SET_EXTENDED_OBJECT_VAR object VPEV 9 list //LinkAreaList
            LIST_ADD list region
            LIST_ADD list region
            
            CREATE_LIST DATATYPE_INT list
            SET_EXTENDED_OBJECT_VAR object VPEV 10 list //LinkNodeList
            LIST_ADD list 0
            LIST_ADD list 0

            CREATE_LIST DATATYPE_INT list
            SET_EXTENDED_OBJECT_VAR object VPEV 11 list //LinkNaviList
            LIST_ADD list 0
            LIST_ADD list 0

            CREATE_LIST DATATYPE_INT list
            SET_EXTENDED_OBJECT_VAR object VPEV 12 list //LinkLengthList
            LIST_ADD list 0
            LIST_ADD list 0

            CREATE_LIST DATATYPE_INT list
            SET_EXTENDED_OBJECT_VAR object VPEV 13 list //LinkObjectList
            LIST_ADD list 0
            LIST_ADD list 0

            //Caso tenha definições de node salvas, cria a node com elas
            GET_LABEL_POINTER savedVariables listptr

            READ_STRUCT_OFFSET listptr 36 4 (count) //Flags
            IF NOT count = 0
                SET_EXTENDED_OBJECT_VAR object VPEV 6 count

                WRITE_STRUCT_OFFSET listptr 32 4 (count) //PathWidth
                IF count < 1024
                    SET_EXTENDED_OBJECT_VAR object VPEV 4 count
                ENDIF
            ENDIF

            linkCount += 2
            nodeCount += 1
        ELSE
            //NaviNodes
            CLEO_CALL getListPointer 0 (2 curfile) (listptr) //NaviList
            READ_MEMORY listptr 4 0 list
            LIST_ADD list object

            GET_CHAR_HEADING scplayer angle
            SET_OBJECT_ROTATION object 90.0 0.0 angle

            flags = 0

            //Seta o número de lanes para 1 para ambos os lados, ruas normais
            CLEO_CALL setFlagFromValue 0 (flags, 8, 10, 1) flags
            CLEO_CALL setFlagFromValue 0 (flags, 11, 13, 1) flags

            INIT_EXTENDED_OBJECT_VARS object VPEV 7

            SET_EXTENDED_OBJECT_VAR object VPEV 1 region //AreaID

            SET_EXTENDED_OBJECT_VAR object VPEV 2 0 //NodeID

            SET_EXTENDED_OBJECT_VAR object VPEV 5 flags //Flags

            //Path Width
            SET_EXTENDED_OBJECT_VAR object VPEV 6 0

            SET_EXTENDED_OBJECT_VAR object VPEV 7 naviCount //NaviID
            naviCount += 1
        ENDIF


        //Seta as variaveis do cabeçalho
        CLEO_CALL getListPointer 0 (3 curfile) (list) //FileList
        READ_MEMORY list 4 0 listptr

        WRITE_STRUCT_OFFSET listptr 0 4 (nodeCount)
        WRITE_STRUCT_OFFSET listptr 4 4 (vehNodeCount)
        WRITE_STRUCT_OFFSET listptr 12 4 (naviCount)
        WRITE_STRUCT_OFFSET listptr 16 4 (linkCount)
    CLEO_RETURN 0
    }

    {
    updateLinkIDs:
        LVAR_INT curfile count size obj list currentid links linkCount

        CLEO_CALL getListPointer 0 (1 curfile) (list) //NodeList
        READ_MEMORY list 4 0 list
        GET_LIST_SIZE list size

        count = 0
        currentid = 0
        linkCount = 0
        WHILE count < size
            GET_LIST_VALUE_BY_INDEX list count (obj)
            IF DOES_OBJECT_EXIST obj
                SET_EXTENDED_OBJECT_VAR obj VPEV 1 currentid
                GET_EXTENDED_OBJECT_VAR obj VPEV 8 links
                currentid += links
                linkCount += links
            ENDIF
            count += 1
        ENDWHILE
    CLEO_RETURN 0 linkCount
    }

    {
    getRegionCurfile:
        LVAR_INT areaid loadedregions count size var

        GET_LIST_SIZE loadedregions size
        count = 0
        WHILE count < size
            GET_LIST_VALUE_BY_INDEX loadedregions count var
            IF var = areaid
                CLEO_RETURN 0 (count)
            ENDIF
            count += 1
        ENDWHILE
    CLEO_RETURN 0 (-1)
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////STREAM MEMORY////////////////////////////////////////////////////////
    stringLabel:
    DUMP
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
    ENDDUMP

    lists:
    DUMP
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 //ObjectLists 0
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 //NodeList 36
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 //NaviList 72
    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 //Files 108
    ENDDUMP

    functionz:
    DUMP
    00 00 00 00
    ENDDUMP

    interregion:
    DUMP
    00 00 00 00
    ENDDUMP

    modelIds:
    DUMP
    00 00 00 00 //0 - Ped
    00 00 00 00 //4 - Vehicle
    00 00 00 00 //8 - Select
    00 00 00 00 //12 - Navi
    ENDDUMP

    savedVariables:
    DUMP
    00 00 00 00 //0 - var1
    00 00 00 00 //4 - var2
    00 00 00 00 //8 - var3
    00 00 00 00 //12 - var4
    00 00 00 00 //16 - var5
    00 00 00 00 //20 - var6
    00 00 00 00 //24 - var7
    00 00 00 00 //28 - var8
    00 00 00 00 //32 - PathWidth
    00 00 00 00 //36 - Flags
    ENDDUMP
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
